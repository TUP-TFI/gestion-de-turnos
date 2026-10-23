package com.grupo140.turnos.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.grupo140.turnos.catalog.CatalogService;
import com.grupo140.turnos.common.error.ApiException;
import com.grupo140.turnos.notification.NotificationService;
import com.grupo140.turnos.user.UserService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    private final Instant now = Instant.parse("2026-10-10T12:00:00Z");
    private final UUID companyId = UUID.randomUUID();
    private final UUID serviceId = UUID.randomUUID();

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private CatalogService catalogService;

    @Mock
    private UserService userService;

    @Mock
    private NotificationService notificationService;

    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentService(
                appointmentRepository,
                catalogService,
                userService,
                notificationService,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void countsPendingAppointmentsAfterNow() {
        when(appointmentRepository.countByCompanyIdAndServiceIdAndStatusAndStartDateTimeAfter(
                        companyId, serviceId, AppointmentStatus.PENDING, now))
                .thenReturn(3L);

        assertThat(appointmentService.countFutureByService(companyId, serviceId))
                .isEqualTo(3L);
    }

    @Test
    void serviceOfAnotherCompanyIsNotCounted() {
        when(catalogService.requireOwnedService(companyId, serviceId))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "SERVICE_NOT_FOUND", "El servicio no existe"));

        assertThatThrownBy(() -> appointmentService.countFutureByService(companyId, serviceId))
                .isInstanceOf(ApiException.class);
        verify(appointmentRepository, never())
                .countByCompanyIdAndServiceIdAndStatusAndStartDateTimeAfter(any(), any(), any(), any());
    }
}
