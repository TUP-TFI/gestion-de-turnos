package com.grupo140.turnos.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import com.grupo140.turnos.common.error.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class PlatformSettingsServiceTest {

    @Mock
    private PlatformSettingsRepository platformSettingsRepository;

    @InjectMocks
    private PlatformSettingsService service;

    // Issue #46. Con los topes de PlatformSettingsRequest (mínima <= 1440 minutos, máxima >= 1 día) esta
    // combinación no llega por HTTP: se prueba el service directo, que es la barrera si los topes cambian.
    @Test
    void minimumNoticeAboveMaximumWindowIsRejected() {
        PlatformSettingsRequest request = new PlatformSettingsRequest(1441, 3, 2, 1);

        assertThatThrownBy(() -> service.update(request)).isInstanceOfSatisfying(ApiException.class, ex -> {
            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getCode()).isEqualTo("VALIDATION_ERROR");
            assertThat(ex.getMessage()).isEqualTo("La anticipación mínima no puede superar a la máxima");
        });
        verifyNoInteractions(platformSettingsRepository);
    }
}
