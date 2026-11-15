package com.grupo140.turnos.seed;

import com.grupo140.turnos.appointment.Appointment;
import com.grupo140.turnos.appointment.AppointmentOrigin;
import com.grupo140.turnos.appointment.AppointmentRepository;
import com.grupo140.turnos.appointment.AppointmentStatus;
import com.grupo140.turnos.appointment.CancelledBy;
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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Datos para la presentación (T-14.2): turnos en todos los estados en Barbería Central y una segunda empresa activa,
 * Estética Lumina, con sus servicios, horario, admin, clientes y turnos. Lo llama {@link DemoDataSeeder} al final, en
 * la misma transacción.
 *
 * <p>Solo corre con {@code app.seed.enabled} y {@code app.seed.demo-data} en {@code true}. Está apagado en los tests,
 * que cuentan con un seed sin turnos y con dos clientes en Barbería Central, y en Render hasta que se decida prenderlo.
 *
 * <p>Se carga una sola vez: si Estética Lumina ya existe, no hace nada. Las fechas son relativas al día en que se carga
 * (hoy, los próximos días hábiles y los anteriores), y cada día lleva sus propios horarios, así los turnos no se
 * superponen. Para cargarlas de nuevo con fechas al día hay que partir de una base vacía.
 */
@Component
@ConditionalOnProperty(
        prefix = "app.seed",
        name = {"enabled", "demo-data"},
        havingValue = "true")
public class PresentationDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(PresentationDataSeeder.class);

    private static final String BARBERIA_SLUG = "barberia-central";
    private static final String ESTETICA_SLUG = "estetica-lumina";
    private static final Set<DayOfWeek> BARBERIA_CLOSED = Set.of(DayOfWeek.SUNDAY);
    private static final Set<DayOfWeek> ESTETICA_CLOSED = Set.of(DayOfWeek.SUNDAY, DayOfWeek.MONDAY);
    private static final String CORTE = "Corte de pelo";
    private static final String CORTE_Y_BARBA = "Corte y barba";
    private static final String LIMPIEZA = "Limpieza facial";
    private static final String MANICURA = "Manicura";
    private static final String MASAJE = "Masaje descontracturante";

    private final CompanyRepository companyRepository;
    private final ServiceRepository serviceRepository;
    private final BusinessHoursRepository businessHoursRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final String password;

    public PresentationDataSeeder(
            CompanyRepository companyRepository,
            ServiceRepository serviceRepository,
            BusinessHoursRepository businessHoursRepository,
            UserRepository userRepository,
            AppointmentRepository appointmentRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.password}") String password) {
        this.companyRepository = companyRepository;
        this.serviceRepository = serviceRepository;
        this.businessHoursRepository = businessHoursRepository;
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.password = password;
    }

    public void seed() {
        if (companyRepository.findBySlug(ESTETICA_SLUG).isPresent()) {
            return;
        }
        Optional<Company> barberia = companyRepository.findBySlug(BARBERIA_SLUG);
        if (barberia.isPresent()) {
            seedBarberiaAppointments(barberia.get());
        }
        seedEsteticaLumina();
        log.info("Seed de demo: turnos de Barbería Central y empresa '{}' creados.", ESTETICA_SLUG);
    }

    // Un turno en cada estado: pendientes (hoy y próximos), reprogramado, manuales con y sin cuenta, cancelados por el
    // cliente y por la empresa, y completados.
    private void seedBarberiaAppointments(Company company) {
        UUID companyId = company.getId();
        Map<String, Service> services = servicesByName(companyId);
        Service corte = services.get(CORTE);
        Service corteYBarba = services.get(CORTE_Y_BARBA);
        Optional<User> lucia = userRepository.findByEmailAndCompanyId("cliente1@example.com", companyId);
        Optional<User> martin = userRepository.findByEmailAndCompanyId("cliente2@example.com", companyId);
        // Si alguien cambió los datos base desde la UI, no hay con qué armar los turnos: se dejan sin cargar.
        if (corte == null || corteYBarba == null || lucia.isEmpty() || martin.isEmpty()) {
            log.warn("Seed de demo: faltan servicios o clientes base de '{}', no se cargan sus turnos.", BARBERIA_SLUG);
            return;
        }
        UUID diego = client(companyId, "Diego", "Fernández", "cliente3@example.com", "+5493510000006");
        UUID camila = client(companyId, "Camila", "López", "cliente4@example.com", "+5493510000007");
        ZoneId zone = ZoneId.of(company.getTimezone());
        Optional<LocalDate> today = openToday(zone, BARBERIA_CLOSED);
        LocalDate next1 = businessDay(zone, BARBERIA_CLOSED, 1);
        LocalDate next2 = businessDay(zone, BARBERIA_CLOSED, 2);
        LocalDate next3 = businessDay(zone, BARBERIA_CLOSED, 3);
        LocalDate next4 = businessDay(zone, BARBERIA_CLOSED, 4);
        LocalDate past1 = businessDay(zone, BARBERIA_CLOSED, -1);
        LocalDate past2 = businessDay(zone, BARBERIA_CLOSED, -2);
        LocalDate past3 = businessDay(zone, BARBERIA_CLOSED, -3);

        if (today.isPresent()) {
            pending(withClient(corte, lucia.get().getId()), at(today.get(), "10:00", zone), AppointmentOrigin.ONLINE);
            pending(
                    withClient(corteYBarba, martin.get().getId()),
                    at(today.get(), "11:00", zone),
                    AppointmentOrigin.ONLINE);
        }
        pending(withClient(corte, diego), at(next1, "09:30", zone), AppointmentOrigin.ONLINE);
        pending(
                withoutAccount(corteYBarba, "Jorge Medina", "+5493510000020"),
                at(next1, "11:00", zone),
                AppointmentOrigin.MANUAL);
        pending(
                rescheduledFrom(withClient(corteYBarba, camila), at(next1, "12:00", zone)),
                at(next2, "10:00", zone),
                AppointmentOrigin.ONLINE);
        pending(withClient(corte, lucia.get().getId()), at(next3, "09:00", zone), AppointmentOrigin.MANUAL);
        cancelled(withClient(corte, martin.get().getId()), at(next3, "12:00", zone), CancelledBy.CLIENT, null);
        cancelled(
                withClient(corteYBarba, diego),
                at(next4, "10:30", zone),
                CancelledBy.COMPANY,
                "El barbero no va a estar ese día");
        completed(withClient(corte, lucia.get().getId()), at(past1, "10:00", zone), AppointmentOrigin.ONLINE);
        completed(withClient(corteYBarba, camila), at(past1, "11:00", zone), AppointmentOrigin.ONLINE);
        completed(
                rescheduledFrom(withClient(corte, martin.get().getId()), at(past3, "12:00", zone)),
                at(past2, "09:30", zone),
                AppointmentOrigin.ONLINE);
        completed(
                withoutAccount(corte, "Rosa Paz", "+5493510000021"),
                at(past3, "10:00", zone),
                AppointmentOrigin.MANUAL);
    }

    // Segunda empresa activa: abre de martes a sábado, de 10 a 18, con su admin, dos clientas y algunos turnos.
    private void seedEsteticaLumina() {
        Company company = new Company("Estética Lumina", ESTETICA_SLUG, CompanyCategory.BEAUTY_CENTER);
        company.setDescription("Tratamientos faciales, manicura y masajes. Empresa de ejemplo para la demo.");
        company.setAddress("Bv. San Juan 850, Córdoba");
        company.setPhone("+5493510000010");
        company.setContactEmail("lumina@example.com");
        company.setPrimaryColor("#7A2368");
        UUID companyId = companyRepository.save(company).getId();

        Service limpieza =
                service(companyId, LIMPIEZA, "Limpieza profunda con extracción e hidratación.", "15000.00", 60);
        Service manicura = service(companyId, MANICURA, "Esmaltado semipermanente.", "9000.00", 30);
        Service masaje = service(companyId, MASAJE, "Espalda, cuello y hombros.", "18000.00", 60);
        for (DayOfWeek day : DayOfWeek.values()) {
            if (!ESTETICA_CLOSED.contains(day)) {
                businessHoursRepository.save(
                        new BusinessHours(companyId, day, LocalTime.of(10, 0), LocalTime.of(18, 0), 30));
            }
        }

        saveActiveUser(
                "Paula", "Díaz", "admin.estetica@example.com", "+5493510000011", UserRole.COMPANY_ADMIN, companyId);
        UUID sofia = client(companyId, "Sofía", "Romero", "sofia@example.com", "+5493510000012");
        UUID valentina = client(companyId, "Valentina", "Ríos", "valentina@example.com", "+5493510000013");

        ZoneId zone = ZoneId.of(company.getTimezone());
        pending(
                withClient(limpieza, sofia),
                at(businessDay(zone, ESTETICA_CLOSED, 1), "10:00", zone),
                AppointmentOrigin.ONLINE);
        pending(
                withClient(masaje, valentina),
                at(businessDay(zone, ESTETICA_CLOSED, 2), "15:00", zone),
                AppointmentOrigin.ONLINE);
        cancelled(
                withClient(manicura, valentina),
                at(businessDay(zone, ESTETICA_CLOSED, 3), "16:00", zone),
                CancelledBy.CLIENT,
                null);
        completed(
                withClient(manicura, sofia),
                at(businessDay(zone, ESTETICA_CLOSED, -1), "11:00", zone),
                AppointmentOrigin.ONLINE);
    }

    private Map<String, Service> servicesByName(UUID companyId) {
        return serviceRepository.findAllByCompanyIdOrderByNameAsc(companyId).stream()
                .collect(Collectors.toMap(Service::getName, Function.identity(), (first, second) -> first));
    }

    private Service service(UUID companyId, String name, String description, String price, int minutes) {
        Service service = new Service(companyId, name, new BigDecimal(price), minutes);
        service.setDescription(description);
        return serviceRepository.save(service);
    }

    // Cliente con cuenta. Si el email ya existe en la empresa, se usa ese.
    private UUID client(UUID companyId, String firstName, String lastName, String email, String phone) {
        return userRepository
                .findByEmailAndCompanyId(email, companyId)
                .map(User::getId)
                .orElseGet(() -> saveActiveUser(firstName, lastName, email, phone, UserRole.CLIENT, companyId));
    }

    private UUID saveActiveUser(
            String firstName, String lastName, String email, String phone, UserRole role, UUID companyId) {
        User user = new User(firstName, lastName, email, phone, role, companyId, UserStatus.ACTIVE);
        user.setPassword(passwordEncoder.encode(password));
        return userRepository.save(user).getId();
    }

    // Hoy, si la empresa abre; si no, no hay turnos "de hoy".
    private static Optional<LocalDate> openToday(ZoneId zone, Set<DayOfWeek> closed) {
        LocalDate today = LocalDate.now(zone);
        return closed.contains(today.getDayOfWeek()) ? Optional.empty() : Optional.of(today);
    }

    // El día hábil número `offset` contando desde hoy: 1 es el próximo día que abre, -1 el último que abrió.
    private static LocalDate businessDay(ZoneId zone, Set<DayOfWeek> closed, int offset) {
        LocalDate date = LocalDate.now(zone);
        int step = offset > 0 ? 1 : -1;
        for (int remaining = Math.abs(offset); remaining > 0; ) {
            date = date.plusDays(step);
            if (!closed.contains(date.getDayOfWeek())) {
                remaining--;
            }
        }
        return date;
    }

    private static Instant at(LocalDate date, String time, ZoneId zone) {
        return date.atTime(LocalTime.parse(time)).atZone(zone).toInstant();
    }

    // Turno con el precio y la duración del servicio copiados, igual que una reserva real. Falta el horario y el
    // estado.
    private static Appointment base(Service service) {
        return new Appointment(
                service.getCompanyId(), service.getId(), service.getPrice(), service.getDurationMinutes());
    }

    private static Appointment withClient(Service service, UUID clientId) {
        Appointment appointment = base(service);
        appointment.setClientId(clientId);
        return appointment;
    }

    private static Appointment withoutAccount(Service service, String name, String phone) {
        Appointment appointment = base(service);
        appointment.setManualClientName(name);
        appointment.setManualClientPhone(phone);
        return appointment;
    }

    // Reprogramado una vez: lo exige chk_appointment_reschedule, con la fecha anterior.
    private static Appointment rescheduledFrom(Appointment appointment, Instant previousStart) {
        appointment.setPreviousStartDateTime(previousStart);
        appointment.setRescheduleCount(1);
        return appointment;
    }

    private void pending(Appointment appointment, Instant start, AppointmentOrigin origin) {
        save(appointment, start, origin, AppointmentStatus.PENDING);
    }

    private void completed(Appointment appointment, Instant start, AppointmentOrigin origin) {
        save(appointment, start, origin, AppointmentStatus.COMPLETED);
    }

    // Lo exige chk_appointment_cancellation: quién canceló y cuándo.
    private void cancelled(Appointment appointment, Instant start, CancelledBy cancelledBy, String reason) {
        appointment.setCancelledBy(cancelledBy);
        appointment.setCancelledAt(Instant.now());
        appointment.setCancellationReason(reason);
        save(appointment, start, AppointmentOrigin.ONLINE, AppointmentStatus.CANCELLED);
    }

    // Una base en uso (local o Render) ya puede tener turnos reales en ese horario. Si choca, se saltea este turno en
    // lugar de romper la constraint excl_appointment_overlap, que haría fallar el arranque.
    private void save(Appointment appointment, Instant start, AppointmentOrigin origin, AppointmentStatus status) {
        Instant end = start.plus(Duration.ofMinutes(appointment.getDurationMinutesSnapshot()));
        if (status != AppointmentStatus.CANCELLED
                && !appointmentRepository
                        .findAllByCompanyIdAndStatusNotAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
                                appointment.getCompanyId(), AppointmentStatus.CANCELLED, end, start)
                        .isEmpty()) {
            log.info("Seed de demo: el horario {} ya está ocupado, se saltea ese turno.", start);
            return;
        }
        appointment.setStartDateTime(start);
        appointment.setEndDateTime(end);
        appointment.setOrigin(origin);
        appointment.setStatus(status);
        appointmentRepository.save(appointment);
    }
}
