package com.grupo140.turnos.schedule;

import com.grupo140.turnos.common.error.ApiException;
import com.grupo140.turnos.company.CompanyService;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio de la plantilla de atención, y API de {@code schedule} para el resto de los features.
 */
@Service
public class ScheduleService {

    // Lunes primero: el enum DayOfWeek ya empieza en lunes.
    private static final Comparator<BusinessHours> BY_DAY_AND_START =
            Comparator.comparing(BusinessHours::getDayOfWeek).thenComparing(BusinessHours::getStartTime);

    private static final Map<DayOfWeek, String> DAY_NAMES = Map.of(
            DayOfWeek.MONDAY, "lunes",
            DayOfWeek.TUESDAY, "martes",
            DayOfWeek.WEDNESDAY, "miércoles",
            DayOfWeek.THURSDAY, "jueves",
            DayOfWeek.FRIDAY, "viernes",
            DayOfWeek.SATURDAY, "sábado",
            DayOfWeek.SUNDAY, "domingo");

    private final BusinessHoursRepository businessHoursRepository;
    private final ScheduleExceptionRepository scheduleExceptionRepository;
    private final CompanyService companyService;
    private final Clock clock;

    public ScheduleService(
            BusinessHoursRepository businessHoursRepository,
            ScheduleExceptionRepository scheduleExceptionRepository,
            CompanyService companyService,
            Clock clock) {
        this.businessHoursRepository = businessHoursRepository;
        this.scheduleExceptionRepository = scheduleExceptionRepository;
        this.companyService = companyService;
        this.clock = clock;
    }

    /** Las franjas de la empresa, ordenadas por día y hora de inicio (US-05.3). */
    @Transactional(readOnly = true)
    public List<BusinessHoursResponse> list(UUID companyId) {
        return businessHoursRepository.findAllByCompanyId(companyId).stream()
                .sorted(BY_DAY_AND_START)
                .map(BusinessHoursResponse::from)
                .toList();
    }

    /**
     * Reemplaza la semana entera de la empresa (US-05.3). Los turnos ya reservados no se tocan, aunque
     * queden fuera del horario nuevo (US-05.4).
     *
     * @throws ApiException {@code INVALID_BUSINESS_HOURS} (400).
     */
    @Transactional
    public List<BusinessHoursResponse> replaceWeek(UUID companyId, BusinessHoursRequest request) {
        validate(request);
        businessHoursRepository.deleteAllByCompanyId(companyId);
        List<BusinessHours> saved = request.hours().stream()
                .map(slot -> businessHoursRepository.save(new BusinessHours(
                        companyId, slot.dayOfWeek(), slot.startTime(), slot.endTime(), slot.intervalMinutes())))
                .toList();
        businessHoursRepository.flush();
        return saved.stream()
                .sorted(BY_DAY_AND_START)
                .map(BusinessHoursResponse::from)
                .toList();
    }

    /**
     * Valida un horario semanal sin guardarlo. Lo usan el guardado y el conteo de turnos fuera de rango, así
     * los dos rechazan los mismos casos. Dos franjas que se tocan (09:00–13:00 y 13:00–17:00) no se
     * superponen, igual que en la constraint {@code excl_hours_overlap} de la base.
     *
     * <p>No se valida que el intervalo entre en la franja: una franja de 09:00 a 09:20 con intervalo de 30
     * minutos es válida, solo que no genera horarios.
     *
     * @throws ApiException {@code INVALID_BUSINESS_HOURS} (400).
     */
    public void validate(BusinessHoursRequest request) {
        for (BusinessHoursRequest.Slot slot : request.hours()) {
            if (!slot.endTime().isAfter(slot.startTime())) {
                throw invalid("En el " + DAY_NAMES.get(slot.dayOfWeek())
                        + ", la hora de fin tiene que ser posterior a la de inicio");
            }
        }

        Map<DayOfWeek, List<BusinessHoursRequest.Slot>> byDay =
                request.hours().stream().collect(Collectors.groupingBy(BusinessHoursRequest.Slot::dayOfWeek));
        byDay.forEach((day, slots) -> {
            List<BusinessHoursRequest.Slot> sorted = slots.stream()
                    .sorted(Comparator.comparing(BusinessHoursRequest.Slot::startTime))
                    .toList();
            for (int i = 1; i < sorted.size(); i++) {
                if (sorted.get(i).startTime().isBefore(sorted.get(i - 1).endTime())) {
                    throw invalid("Las franjas del " + DAY_NAMES.get(day) + " se superponen");
                }
            }
        });
    }

    /** Las excepciones de calendario de hoy en adelante, de la más cercana a la más lejana (US-05.6). */
    @Transactional(readOnly = true)
    public List<ScheduleExceptionResponse> listExceptions(UUID companyId) {
        return scheduleExceptionRepository
                .findAllByCompanyIdAndExceptionDateGreaterThanEqualOrderByExceptionDateAsc(companyId, today(companyId))
                .stream()
                .map(ScheduleExceptionResponse::from)
                .toList();
    }

    /**
     * Alta de una excepción de calendario (US-05.6). Los turnos ya reservados en esa fecha no se tocan.
     *
     * @throws ApiException {@code INVALID_SCHEDULE_EXCEPTION} (400) o {@code SCHEDULE_EXCEPTION_DATE_TAKEN}
     *     (409).
     */
    @Transactional
    public ScheduleExceptionResponse createException(UUID companyId, ScheduleExceptionRequest request) {
        validateException(companyId, request);
        if (scheduleExceptionRepository.existsByCompanyIdAndExceptionDate(companyId, request.exceptionDate())) {
            throw dateTaken();
        }
        ScheduleException exception = new ScheduleException(companyId, request.exceptionDate());
        applyShape(exception, request);
        return ScheduleExceptionResponse.from(scheduleExceptionRepository.saveAndFlush(exception));
    }

    /**
     * Edición de una excepción de calendario (US-05.6). Los turnos ya reservados en esa fecha no se tocan.
     *
     * @throws ApiException {@code SCHEDULE_EXCEPTION_NOT_FOUND} (404), {@code INVALID_SCHEDULE_EXCEPTION} (400)
     *     o {@code SCHEDULE_EXCEPTION_DATE_TAKEN} (409).
     */
    @Transactional
    public ScheduleExceptionResponse updateException(
            UUID companyId, UUID exceptionId, ScheduleExceptionRequest request) {
        ScheduleException exception = requireOwnedException(companyId, exceptionId);
        validateException(companyId, request);
        if (scheduleExceptionRepository.existsByCompanyIdAndExceptionDateAndIdNot(
                companyId, request.exceptionDate(), exceptionId)) {
            throw dateTaken();
        }
        exception.setExceptionDate(request.exceptionDate());
        applyShape(exception, request);
        return ScheduleExceptionResponse.from(scheduleExceptionRepository.saveAndFlush(exception));
    }

    /**
     * Borra una excepción de calendario (US-05.6); esa fecha vuelve a regirse por el horario semanal.
     *
     * @throws ApiException {@code SCHEDULE_EXCEPTION_NOT_FOUND} (404).
     */
    @Transactional
    public void deleteException(UUID companyId, UUID exceptionId) {
        scheduleExceptionRepository.delete(requireOwnedException(companyId, exceptionId));
    }

    /**
     * Valida una excepción sin guardarla. Lo usan el guardado y el conteo de turnos afectados, así los dos
     * rechazan los mismos casos. "Hoy" se calcula en la zona horaria de la empresa, no en la del servidor.
     *
     * @throws ApiException {@code INVALID_SCHEDULE_EXCEPTION} (400).
     */
    public void validateException(UUID companyId, ScheduleExceptionRequest request) {
        if (request.exceptionDate().isBefore(today(companyId))) {
            throw invalidException("La fecha no puede ser anterior a hoy");
        }
        boolean hasAnyTime =
                request.startTime() != null || request.endTime() != null || request.intervalMinutes() != null;
        if (request.isClosed()) {
            if (hasAnyTime) {
                throw invalidException("Un día cerrado no lleva horario");
            }
            return;
        }
        if (request.startTime() == null || request.endTime() == null || request.intervalMinutes() == null) {
            throw invalidException("Para abrir con horario especial cargá la hora de inicio, la de fin y el intervalo");
        }
        if (!request.endTime().isAfter(request.startTime())) {
            throw invalidException("La hora de fin tiene que ser posterior a la de inicio");
        }
    }

    /**
     * Las franjas de atención de una fecha, ordenadas por hora de inicio (US-05.5). Si la fecha tiene una
     * {@link ScheduleException}, reemplaza por completo al horario semanal: cerrada devuelve una lista vacía, y
     * con horario especial devuelve una sola franja. Sin excepción, devuelve las franjas semanales del día de la
     * semana de la fecha. No sabe nada de los turnos: es la base de la que parte {@code AvailabilityService}.
     */
    @Transactional(readOnly = true)
    public List<DayWindow> windowsFor(UUID companyId, LocalDate date) {
        Optional<ScheduleException> exception =
                scheduleExceptionRepository.findByCompanyIdAndExceptionDate(companyId, date);
        if (exception.isPresent()) {
            ScheduleException found = exception.get();
            if (found.isClosed()) {
                return List.of();
            }
            return List.of(new DayWindow(found.getStartTime(), found.getEndTime(), found.getIntervalMinutes()));
        }
        return businessHoursRepository.findAllByCompanyId(companyId).stream()
                .filter(slot -> slot.getDayOfWeek() == date.getDayOfWeek())
                .sorted(Comparator.comparing(BusinessHours::getStartTime))
                .map(slot -> new DayWindow(slot.getStartTime(), slot.getEndTime(), slot.getIntervalMinutes()))
                .toList();
    }

    /**
     * Fechas con excepción de hoy en adelante. La usa el aviso de turnos fuera de rango (US-05.4): un turno en
     * una de esas fechas se rige por la excepción, no por el horario semanal. Solo devuelve las fechas; para
     * saber qué horario rige en una fecha puntual, ver {@link #windowsFor}.
     */
    @Transactional(readOnly = true)
    public Set<LocalDate> upcomingExceptionDates(UUID companyId) {
        return scheduleExceptionRepository
                .findAllByCompanyIdAndExceptionDateGreaterThanEqualOrderByExceptionDateAsc(companyId, today(companyId))
                .stream()
                .map(ScheduleException::getExceptionDate)
                .collect(Collectors.toSet());
    }

    private LocalDate today(UUID companyId) {
        return LocalDate.now(clock.withZone(companyService.zoneOf(companyId)));
    }

    private ScheduleException requireOwnedException(UUID companyId, UUID exceptionId) {
        return scheduleExceptionRepository
                .findByIdAndCompanyId(exceptionId, companyId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "SCHEDULE_EXCEPTION_NOT_FOUND", "La excepción no existe"));
    }

    private static void applyShape(ScheduleException exception, ScheduleExceptionRequest request) {
        if (request.isClosed()) {
            exception.closeAllDay();
        } else {
            exception.openWith(request.startTime(), request.endTime(), request.intervalMinutes());
        }
        exception.setReason(blankToNull(request.reason()));
    }

    // Un motivo vacío o con solo espacios se guarda como "sin motivo".
    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_BUSINESS_HOURS", message);
    }

    private static ApiException invalidException(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SCHEDULE_EXCEPTION", message);
    }

    private static ApiException dateTaken() {
        return new ApiException(
                HttpStatus.CONFLICT, "SCHEDULE_EXCEPTION_DATE_TAKEN", "Ya hay una excepción cargada para esa fecha");
    }
}
