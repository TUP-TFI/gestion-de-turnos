package com.grupo140.turnos.reporting;

import com.grupo140.turnos.appointment.AppointmentService;
import com.grupo140.turnos.company.CompanyService;
import com.grupo140.turnos.company.CompanyStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dashboard del superadmin (US-04.6). Junta conteos de varios features a través de sus services, nunca de
 * sus repositories (regla de dependencias del README).
 */
@Service
public class SuperadminStatsService {

    private final CompanyService companyService;
    private final AppointmentService appointmentService;

    public SuperadminStatsService(CompanyService companyService, AppointmentService appointmentService) {
        this.companyService = companyService;
        this.appointmentService = appointmentService;
    }

    @Transactional(readOnly = true)
    public SuperadminStatsResponse get() {
        return new SuperadminStatsResponse(
                companyService.countByStatus(CompanyStatus.ACTIVE),
                companyService.countByStatus(CompanyStatus.INACTIVE),
                appointmentService.countAll());
    }
}
