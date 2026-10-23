package com.grupo140.turnos.schedule;

import com.grupo140.turnos.common.error.ApiException;
import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

    public ScheduleService(BusinessHoursRepository businessHoursRepository) {
        this.businessHoursRepository = businessHoursRepository;
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

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_BUSINESS_HOURS", message);
    }
}
