package com.grupo140.turnos.company;

import com.grupo140.turnos.auth.ActivationService;
import com.grupo140.turnos.common.error.ApiException;
import com.grupo140.turnos.common.security.CompanyStatusChecker;
import com.grupo140.turnos.notification.NotificationService;
import com.grupo140.turnos.user.CompanyAdmin;
import com.grupo140.turnos.user.UserService;
import com.grupo140.turnos.user.UserStatus;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio de las empresas, y API de {@code company} para el resto de los features.
 */
@Service
public class CompanyService implements CompanyStatusChecker {

    private final CompanyRepository companyRepository;
    private final SlugGenerator slugGenerator;
    private final UserService userService;
    private final ActivationService activationService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher events;
    private final String frontendUrl;

    public CompanyService(
            CompanyRepository companyRepository,
            SlugGenerator slugGenerator,
            UserService userService,
            ActivationService activationService,
            NotificationService notificationService,
            ApplicationEventPublisher events,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.companyRepository = companyRepository;
        this.slugGenerator = slugGenerator;
        this.userService = userService;
        this.activationService = activationService;
        this.notificationService = notificationService;
        this.events = events;
        this.frontendUrl = frontendUrl;
    }

    /** Indica si la empresa existe y está {@link CompanyStatus#ACTIVE}. */
    @Override
    @Transactional(readOnly = true)
    public boolean isActive(UUID companyId) {
        return companyRepository
                .findStatusById(companyId)
                .map(status -> status == CompanyStatus.ACTIVE)
                .orElse(false);
    }

    /**
     * Id de la empresa dueña de una página pública, para el registro y el login de clientes (US-03.5 y
     * US-03.6), que llegan por el slug y no tienen sesión todavía.
     *
     * @throws ApiException {@code COMPANY_NOT_FOUND} (404) o {@code COMPANY_INACTIVE} (403).
     */
    @Transactional(readOnly = true)
    public UUID requireActiveIdBySlug(String slug) {
        Company company = companyRepository
                .findBySlug(slug)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND", "La empresa no existe"));
        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "COMPANY_INACTIVE", "La empresa está desactivada");
        }
        return company.getId();
    }

    /**
     * Zona horaria de la empresa, con la que se interpretan las horas de su horario de atención. La usan el
     * aviso de turnos fuera de rango (US-05.4) y el cálculo de disponibilidad (US-05.5).
     *
     * @throws ApiException {@code COMPANY_NOT_FOUND} (404).
     */
    @Transactional(readOnly = true)
    public ZoneId zoneOf(UUID companyId) {
        return ZoneId.of(findOrThrow(companyId).getTimezone());
    }

    /** Cantidad de empresas en un estado, para el dashboard del superadmin (US-04.6). */
    @Transactional(readOnly = true)
    public long countByStatus(CompanyStatus status) {
        return companyRepository.countByStatus(status);
    }

    /**
     * Alta de empresa (US-04.1): crea la empresa, su admin pendiente de activación y el token del link,
     * todo en una sola transacción. Si algo falla, no queda nada guardado.
     *
     * @throws ApiException {@code EMAIL_ALREADY_IN_USE} (409) o {@code VALIDATION_ERROR} (400).
     */
    @Transactional
    public CompanyResponse create(CreateCompanyRequest request) {
        CreateCompanyRequest.CompanyData data = request.company();
        CreateCompanyRequest.AdminData admin = request.admin();

        if (userService.isPanelEmailTaken(admin.email())) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "EMAIL_ALREADY_IN_USE", "Ya existe un administrador con ese email");
        }
        requireValidTimezone(data.timezone());

        Company company = new Company(data.name(), slugGenerator.generate(data.name()), data.category());
        applyData(company, data);
        company = companyRepository.saveAndFlush(company);

        UUID adminId = userService.createPendingAdmin(
                company.getId(), admin.firstName(), admin.lastName(), admin.email(), admin.phone());
        String token = activationService.createActivationToken(adminId);
        notificationService.sendActivationLink(admin.phone(), activationLink(token));

        return CompanyResponse.from(company);
    }

    /**
     * Reenvío del link de activación al admin que todavía no activó su cuenta (US-04.8). El link anterior
     * deja de servir.
     *
     * @throws ApiException {@code COMPANY_NOT_FOUND} (404) o {@code ADMIN_ALREADY_ACTIVE} (409).
     */
    @Transactional
    public void resendActivation(UUID id) {
        findOrThrow(id);
        CompanyAdmin admin = userService
                .findCompanyAdmin(id)
                .orElseThrow(() -> new IllegalStateException("La empresa " + id + " no tiene admin"));
        if (admin.status() != UserStatus.PENDING_ACTIVATION) {
            throw new ApiException(HttpStatus.CONFLICT, "ADMIN_ALREADY_ACTIVE", "El admin ya activó su cuenta");
        }

        String token = activationService.renewActivationToken(admin.id());
        notificationService.sendActivationLink(admin.phone(), activationLink(token));
    }

    /** Listado de todas las empresas para el superadmin, ordenado por nombre (US-04.2). */
    @Transactional(readOnly = true)
    public List<CompanyResponse> list() {
        return companyRepository.findAllByOrderByNameAsc().stream()
                .map(CompanyResponse::from)
                .toList();
    }

    /**
     * Detalle de una empresa, para cargar el formulario de edición (US-04.3).
     *
     * @throws ApiException {@code COMPANY_NOT_FOUND} (404).
     */
    @Transactional(readOnly = true)
    public CompanyDetailResponse get(UUID id) {
        Company company = findOrThrow(id);
        CompanyDetailResponse.AdminSummary admin = userService
                .findCompanyAdmin(id)
                .map(found -> new CompanyDetailResponse.AdminSummary(found.email(), found.status()))
                .orElse(null);
        return new CompanyDetailResponse(CompanyResponse.from(company), admin);
    }

    /**
     * Edición de los datos de una empresa (US-04.3). El slug no cambia aunque cambie el nombre, porque
     * forma parte de links ya compartidos; el estado se maneja aparte (US-04.4 y US-04.5).
     *
     * @throws ApiException {@code COMPANY_NOT_FOUND} (404) o {@code VALIDATION_ERROR} (400).
     */
    @Transactional
    public CompanyResponse update(UUID id, CreateCompanyRequest.CompanyData data) {
        requireValidTimezone(data.timezone());

        Company company = findOrThrow(id);
        applyData(company, data);
        return CompanyResponse.from(companyRepository.saveAndFlush(company));
    }

    /**
     * Desactiva una empresa (US-04.4): nunca se borra, pasa a {@link CompanyStatus#INACTIVE}. Publica
     * {@link CompanyDeactivatedEvent} para que {@code appointment} cancele sus turnos pendientes en la misma
     * transacción. El login del admin queda bloqueado solo, porque se deriva del estado de la empresa.
     *
     * <p>Si ya estaba inactiva, no hace nada y devuelve la empresa igual.
     *
     * @throws ApiException {@code COMPANY_NOT_FOUND} (404).
     */
    @Transactional
    public CompanyResponse deactivate(UUID id) {
        Company company = findOrThrow(id);
        if (company.getStatus() == CompanyStatus.INACTIVE) {
            return CompanyResponse.from(company);
        }

        company.setStatus(CompanyStatus.INACTIVE);
        company = companyRepository.saveAndFlush(company);
        events.publishEvent(new CompanyDeactivatedEvent(id));
        return CompanyResponse.from(company);
    }

    /**
     * Reactiva una empresa (US-04.5): vuelve a {@link CompanyStatus#ACTIVE} con la configuración que ya tenía.
     * Los turnos cancelados al desactivarla no se recuperan.
     *
     * <p>Si ya estaba activa, no hace nada y devuelve la empresa igual.
     *
     * @throws ApiException {@code COMPANY_NOT_FOUND} (404).
     */
    @Transactional
    public CompanyResponse reactivate(UUID id) {
        Company company = findOrThrow(id);
        if (company.getStatus() == CompanyStatus.ACTIVE) {
            return CompanyResponse.from(company);
        }

        company.setStatus(CompanyStatus.ACTIVE);
        return CompanyResponse.from(companyRepository.saveAndFlush(company));
    }

    private String activationLink(String token) {
        return frontendUrl + "/activate?token=" + token;
    }

    private Company findOrThrow(UUID id) {
        return companyRepository
                .findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND", "La empresa no existe"));
    }

    // Campos editables, los mismos en el alta y en la edición. El slug y el estado quedan afuera.
    private static void applyData(Company company, CreateCompanyRequest.CompanyData data) {
        company.setName(data.name());
        company.setCategory(data.category());
        company.setTimezone(data.timezone());
        company.setDescription(data.description());
        company.setAddress(data.address());
        company.setPhone(data.phone());
        company.setContactEmail(data.contactEmail());
        company.setLogoUrl(data.logoUrl());
    }

    // Es válida si es una zona IANA que Java conoce: con ella se calcula después la disponibilidad.
    private static void requireValidTimezone(String timezone) {
        if (!ZoneId.getAvailableZoneIds().contains(timezone)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "La zona horaria no es válida");
        }
    }
}
