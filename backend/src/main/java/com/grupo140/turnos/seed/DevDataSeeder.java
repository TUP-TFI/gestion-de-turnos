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
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga los datos mínimos para probar el login, la activación de cuenta, los servicios y los horarios
 * sin tener que crear esos datos a mano.
 *
 * <p>Corre al arrancar solo si se cumplen dos condiciones: el perfil es {@code dev} y además se pide
 * con {@code app.seed.enabled=true}.
 *
 * <p>Decisiones:
 *
 * <ul>
 *   <li>El perfil solo no alcanza porque {@code dev} es el perfil por defecto: si en producción faltara
 *       {@code SPRING_PROFILES_ACTIVE}, la aplicación arrancaría en {@code dev} contra la base real y
 *       crearía usuarios con una contraseña que está escrita en este archivo. Por eso
 *       {@code app.seed.enabled} no se define en ningún {@code application*.yml}: se pasa a mano al
 *       arrancar en local.
 *   <li>Si la empresa de ejemplo ya existe, no hace nada: un segundo arranque no duplica datos.
 *   <li>Todo se guarda en una sola transacción: si algo falla, no queda un seed a medias.
 *   <li>Las contraseñas se encriptan con el {@link PasswordEncoder} del sistema, el mismo del login.
 *   <li>El token de activación tiene un valor fijo ({@link #DEV_ACTIVATION_TOKEN}) y no uno aleatorio,
 *       para poder probar la activación aunque la base se haya cargado hace días. Por lo mismo vence
 *       en un año y no en 48 horas.
 *   <li>No toca {@code platform_settings}: esa fila la inserta la migración V1.
 * </ul>
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    // Contraseña de todos los usuarios de prueba. Solo existe en dev.
    public static final String DEV_PASSWORD = "Turnos.dev.2026";

    // Valor en claro del token de activación del admin pendiente. Solo existe en dev.
    static final String DEV_ACTIVATION_TOKEN = "dev-activation-token";

    private static final String COMPANY_SLUG = "barberia-central";
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

    public DevDataSeeder(
            CompanyRepository companyRepository,
            ServiceRepository serviceRepository,
            BusinessHoursRepository businessHoursRepository,
            UserRepository userRepository,
            UserTokenRepository userTokenRepository,
            PasswordEncoder passwordEncoder,
            TokenGenerator tokenGenerator) {
        this.companyRepository = companyRepository;
        this.serviceRepository = serviceRepository;
        this.businessHoursRepository = businessHoursRepository;
        this.userRepository = userRepository;
        this.userTokenRepository = userTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (companyRepository.existsBySlug(COMPANY_SLUG)) {
            log.info("Seed de dev: la empresa '{}' ya existe, no se carga nada.", COMPANY_SLUG);
            return;
        }

        UUID companyId = seedCompany();
        seedServices(companyId);
        seedBusinessHours(companyId);
        seedUsers(companyId);

        log.info("Seed de dev cargado: empresa '{}', 2 servicios, horario semanal y 5 usuarios.", COMPANY_SLUG);
        log.info("Seed de dev: token de activación del admin pendiente = {}", DEV_ACTIVATION_TOKEN);
    }

    private UUID seedCompany() {
        Company company = new Company("Barbería Central", COMPANY_SLUG, CompanyCategory.BARBERSHOP);
        company.setDescription("Empresa de ejemplo para desarrollo.");
        company.setAddress("Av. Colón 1234, Córdoba");
        company.setPhone("+5493510000000");
        company.setContactEmail("contacto@example.com");
        company.setPrimaryColor("#1F2937");
        return companyRepository.save(company).getId();
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

    // Emails de example.com y teléfonos inventados: no pertenecen a nadie.
    private void seedUsers(UUID companyId) {
        saveActiveUser("Ana", "Superadmin", "superadmin@example.com", "+5493510000001", UserRole.SUPERADMIN, null);
        saveActiveUser("Carlos", "Gómez", "admin@example.com", "+5493510000002", UserRole.COMPANY_ADMIN, companyId);
        saveActiveUser("Lucía", "Pérez", "cliente1@example.com", "+5493510000004", UserRole.CLIENT, companyId);
        saveActiveUser("Martín", "Sosa", "cliente2@example.com", "+5493510000005", UserRole.CLIENT, companyId);

        // Admin recién creado que todavía no activó su cuenta: no tiene contraseña, la define al activarla.
        // El token es lo que viaja en el link de activación; en la base se guarda solo su hash.
        User pendingAdmin = userRepository.save(new User(
                "Marta",
                "Ruiz",
                "admin.pendiente@example.com",
                "+5493510000003",
                UserRole.COMPANY_ADMIN,
                companyId,
                UserStatus.PENDING_ACTIVATION));
        userTokenRepository.save(new UserToken(
                pendingAdmin.getId(),
                tokenGenerator.hash(DEV_ACTIVATION_TOKEN),
                TokenType.ACTIVATION,
                Instant.now().plus(365, ChronoUnit.DAYS)));
    }

    private void saveActiveUser(
            String firstName, String lastName, String email, String phone, UserRole role, UUID companyId) {
        User user = new User(firstName, lastName, email, phone, role, companyId, UserStatus.ACTIVE);
        user.setPassword(passwordEncoder.encode(DEV_PASSWORD));
        userRepository.save(user);
    }
}
