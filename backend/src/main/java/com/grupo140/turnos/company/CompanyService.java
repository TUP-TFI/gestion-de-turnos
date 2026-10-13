package com.grupo140.turnos.company;

import com.grupo140.turnos.auth.ActivationService;
import com.grupo140.turnos.common.error.ApiException;
import com.grupo140.turnos.notification.NotificationService;
import com.grupo140.turnos.user.UserService;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio de las empresas, y API de {@code company} para el resto de los features.
 */
@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final SlugGenerator slugGenerator;
    private final UserService userService;
    private final ActivationService activationService;
    private final NotificationService notificationService;
    private final String frontendUrl;

    public CompanyService(
            CompanyRepository companyRepository,
            SlugGenerator slugGenerator,
            UserService userService,
            ActivationService activationService,
            NotificationService notificationService,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.companyRepository = companyRepository;
        this.slugGenerator = slugGenerator;
        this.userService = userService;
        this.activationService = activationService;
        this.notificationService = notificationService;
        this.frontendUrl = frontendUrl;
    }

    /** Indica si la empresa existe y está {@link CompanyStatus#ACTIVE}. */
    @Transactional(readOnly = true)
    public boolean isActive(UUID companyId) {
        return companyRepository
                .findStatusById(companyId)
                .map(status -> status == CompanyStatus.ACTIVE)
                .orElse(false);
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
        if (!isValidTimezone(data.timezone())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "La zona horaria no es válida");
        }

        Company company = new Company(data.name(), slugGenerator.generate(data.name()), data.category());
        company.setTimezone(data.timezone());
        company.setDescription(data.description());
        company.setAddress(data.address());
        company.setPhone(data.phone());
        company.setContactEmail(data.contactEmail());
        company.setLogoUrl(data.logoUrl());
        company = companyRepository.saveAndFlush(company);

        UUID adminId = userService.createPendingAdmin(
                company.getId(), admin.firstName(), admin.lastName(), admin.email(), admin.phone());
        String token = activationService.createActivationToken(adminId);
        notificationService.sendActivationLink(admin.phone(), frontendUrl + "/activate?token=" + token);

        return CompanyResponse.from(company);
    }

    // Es válida si es una zona IANA que Java conoce: con ella se calcula después la disponibilidad.
    private static boolean isValidTimezone(String timezone) {
        return ZoneId.getAvailableZoneIds().contains(timezone);
    }
}
