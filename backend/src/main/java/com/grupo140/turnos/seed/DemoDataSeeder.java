package com.grupo140.turnos.seed;

import com.grupo140.turnos.auth.TokenGenerator;
import com.grupo140.turnos.auth.TokenType;
import com.grupo140.turnos.auth.UserToken;
import com.grupo140.turnos.auth.UserTokenRepository;
import com.grupo140.turnos.catalog.Service;
import com.grupo140.turnos.catalog.ServiceRepository;
import com.grupo140.turnos.company.Company;
import com.grupo140.turnos.company.CompanyCategory;
import com.grupo140.turnos.company.CompanyRepository;
import com.grupo140.turnos.schedule.BusinessHours;
import com.grupo140.turnos.schedule.BusinessHoursRepository;
import com.grupo140.turnos.user.User;
import com.grupo140.turnos.user.UserRepository;
import com.grupo140.turnos.user.UserRole;
import com.grupo140.turnos.user.UserStatus;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga los datos de prueba para usar el sistema sin crearlos a mano: en local, en los tests y en el
 * despliegue que revisa el tutor.
 *
 * <p>Corre al arrancar solo si se pide con {@code app.seed.enabled=true}, en cualquier perfil.
 *
 * <p>Decisiones:
 *
 * <ul>
 *   <li>La contraseña de los usuarios activos ({@code app.seed.password}) y el token de activación del
 *       admin pendiente ({@code app.seed.activation-token}) no están en el código. En {@code dev} tienen
 *       valores fijos en {@code application-dev.yml}; en {@code prod} se leen de variables de entorno sin
 *       valor por defecto. Así, en Neon nunca queda una contraseña que esté escrita en el repositorio.
 *   <li>{@code app.seed.enabled} no se define en ningún {@code application*.yml}: se pasa a mano en local
 *       y como variable de entorno en Render. Apagarlo es sacar la variable.
 *   <li>Cada bloque revisa su propia clave (slug de la empresa, email del usuario) y crea solo lo que
 *       falta. Una funcionalidad nueva suma su bloque, y el próximo arranque lo carga en una base que ya
 *       tenía el resto, sin duplicar ni pisar lo que alguien haya cambiado desde la UI.
 *   <li>Los servicios y el horario de una empresa se crean solo junto con la empresa. Si alguien los
 *       borra después, no vuelven a aparecer.
 *   <li>Todo se guarda en una sola transacción: si algo falla, no queda un seed a medias.
 *   <li>Las contraseñas se encriptan con el {@link PasswordEncoder} del sistema, el mismo del login.
 *   <li>El token de activación tiene un valor conocido y no uno aleatorio, para poder probar la
 *       activación aunque la base se haya cargado hace días. Por lo mismo vence en un año y no en 48 horas.
 *   <li>No toca {@code platform_settings}: esa fila la inserta la migración V1.
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    // Mismo mínimo que la activación de cuenta: la contraseña del seed tiene que poder elegirse a mano.
    private static final int MIN_PASSWORD_LENGTH = 8;

    private static final String SUPERADMIN_EMAIL = "superadmin@example.com";
    private static final String COMPANY_SLUG = "barberia-central";
    // Empresa recién dada de alta, cuyo admin todavía no activó la cuenta. Cada empresa tiene un solo admin.
    private static final String PENDING_COMPANY_SLUG = "estetica-pendiente";
    private static final String PENDING_ADMIN_EMAIL = "admin.pendiente@example.com";
    private static final int INTERVAL_MINUTES = 30;
    private static final LocalTime MORNING_START = LocalTime.of(9, 0);
    private static final LocalTime MORNING_END = LocalTime.of(13, 0);
    private static final LocalTime AFTERNOON_START = LocalTime.of(16, 0);
    private static final LocalTime AFTERNOON_END = LocalTime.of(20, 0);

    private final CompanyRepository companyRepository;
    private final ServiceRepository serviceRepository;
    private final BusinessHoursRepository businessHoursRepository;
    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenGenerator tokenGenerator;
    private final String password;
    private final String activationToken;

    public DemoDataSeeder(
            CompanyRepository companyRepository,
            ServiceRepository serviceRepository,
            BusinessHoursRepository businessHoursRepository,
            UserRepository userRepository,
            UserTokenRepository userTokenRepository,
            PasswordEncoder passwordEncoder,
            TokenGenerator tokenGenerator,
            @Value("${app.seed.password}") String password,
            @Value("${app.seed.activation-token}") String activationToken) {
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "app.seed.password tiene que tener al menos " + MIN_PASSWORD_LENGTH + " caracteres");
        }
        if (activationToken.isBlank()) {
            throw new IllegalStateException("app.seed.activation-token no puede estar vacío");
        }
        this.companyRepository = companyRepository;
        this.serviceRepository = serviceRepository;
        this.businessHoursRepository = businessHoursRepository;
        this.userRepository = userRepository;
        this.userTokenRepository = userTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenGenerator = tokenGenerator;
        this.password = password;
        this.activationToken = activationToken;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedSuperadmin();
        seedBarberiaCentral();
        seedPendingCompany();
        log.info("Seed de demo revisado: se creó solo lo que faltaba.");
    }

    private void seedSuperadmin() {
        if (userRepository.findSuperadminByEmail(SUPERADMIN_EMAIL).isPresent()) {
            return;
        }
        saveActiveUser("Ana", "Superadmin", SUPERADMIN_EMAIL, "+5493510000001", UserRole.SUPERADMIN, null);
        log.info("Seed de demo: superadmin '{}' creado.", SUPERADMIN_EMAIL);
    }

    private void seedBarberiaCentral() {
        UUID companyId =
                companyRepository.findBySlug(COMPANY_SLUG).map(Company::getId).orElseGet(this::createBarberiaCentral);

        // Emails de example.com y teléfonos inventados: no pertenecen a nadie.
        seedActiveUser("Carlos", "Gómez", "admin@example.com", "+5493510000002", UserRole.COMPANY_ADMIN, companyId);
        seedActiveUser("Lucía", "Pérez", "cliente1@example.com", "+5493510000004", UserRole.CLIENT, companyId);
        seedActiveUser("Martín", "Sosa", "cliente2@example.com", "+5493510000005", UserRole.CLIENT, companyId);
    }

    private UUID createBarberiaCentral() {
        Company company = new Company("Barbería Central", COMPANY_SLUG, CompanyCategory.BARBERSHOP);
        company.setDescription("Empresa de ejemplo para desarrollo.");
        company.setAddress("Av. Colón 1234, Córdoba");
        company.setPhone("+5493510000000");
        company.setContactEmail("contacto@example.com");
        company.setPrimaryColor("#1F2937");
        UUID companyId = companyRepository.save(company).getId();
        seedServices(companyId);
        seedBusinessHours(companyId);
        log.info("Seed de demo: empresa '{}' creada con 2 servicios y horario semanal.", COMPANY_SLUG);
        return companyId;
    }

    // Duraciones distintas a propósito: con intervalo de 30, el servicio de 60 ocupa dos franjas.
    private void seedServices(UUID companyId) {
        serviceRepository.save(new Service(companyId, "Corte de pelo", new BigDecimal("8000.00"), 30));
        serviceRepository.save(new Service(companyId, "Corte y barba", new BigDecimal("12000.00"), 60));
    }

    // Lunes a viernes con jornada partida, sábado solo a la mañana, domingo sin filas (cerrado).
    private void seedBusinessHours(UUID companyId) {
        List<DayOfWeek> weekdays =
                List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
        for (DayOfWeek day : weekdays) {
            saveHours(companyId, day, MORNING_START, MORNING_END);
            saveHours(companyId, day, AFTERNOON_START, AFTERNOON_END);
        }
        saveHours(companyId, DayOfWeek.SATURDAY, MORNING_START, MORNING_END);
    }

    private void saveHours(UUID companyId, DayOfWeek day, LocalTime start, LocalTime end) {
        businessHoursRepository.save(new BusinessHours(companyId, day, start, end, INTERVAL_MINUTES));
    }

    // Como queda una empresa recién dada de alta: sin servicios ni horarios, y con su admin pendiente de activación.
    private void seedPendingCompany() {
        UUID companyId = companyRepository
                .findBySlug(PENDING_COMPANY_SLUG)
                .map(Company::getId)
                .orElseGet(() -> companyRepository
                        .save(new Company("Estética Pendiente", PENDING_COMPANY_SLUG, CompanyCategory.BEAUTY_CENTER))
                        .getId());
        if (userRepository
                .findByEmailAndCompanyId(PENDING_ADMIN_EMAIL, companyId)
                .isPresent()) {
            return;
        }

        // Admin que todavía no activó su cuenta: no tiene contraseña, la define al activarla.
        // El token es lo que viaja en el link de activación; en la base se guarda solo su hash.
        User pendingAdmin = userRepository.save(new User(
                "Marta",
                "Ruiz",
                PENDING_ADMIN_EMAIL,
                "+5493510000003",
                UserRole.COMPANY_ADMIN,
                companyId,
                UserStatus.PENDING_ACTIVATION));
        userTokenRepository.save(new UserToken(
                pendingAdmin.getId(),
                tokenGenerator.hash(activationToken),
                TokenType.ACTIVATION,
                Instant.now().plus(365, ChronoUnit.DAYS)));
        log.info("Seed de demo: empresa '{}' con admin pendiente de activación creada.", PENDING_COMPANY_SLUG);
    }

    private void seedActiveUser(
            String firstName, String lastName, String email, String phone, UserRole role, UUID companyId) {
        if (userRepository.findByEmailAndCompanyId(email, companyId).isPresent()) {
            return;
        }
        saveActiveUser(firstName, lastName, email, phone, role, companyId);
        log.info("Seed de demo: usuario '{}' ({}) creado.", email, role);
    }

    private void saveActiveUser(
            String firstName, String lastName, String email, String phone, UserRole role, UUID companyId) {
        User user = new User(firstName, lastName, email, phone, role, companyId, UserStatus.ACTIVE);
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }
}
