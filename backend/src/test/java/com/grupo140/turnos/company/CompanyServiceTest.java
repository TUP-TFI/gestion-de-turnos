package com.grupo140.turnos.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.grupo140.turnos.auth.ActivationService;
import com.grupo140.turnos.common.error.ApiException;
import com.grupo140.turnos.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

/**
 * Alta de empresa (US-04.1) cuando la base rechaza el slug. Dos altas simultáneas no se pueden reproducir de forma
 * confiable en un test de integración, así que se simula el rechazo de la base.
 */
@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private SlugGenerator slugGenerator;

    @Mock
    private UserService userService;

    @Mock
    private ActivationService activationService;

    @Mock
    private ApplicationEventPublisher events;

    private CompanyService companyService;

    @BeforeEach
    void setUp() {
        companyService = new CompanyService(
                companyRepository, slugGenerator, userService, activationService, events, "http://localhost:5173");
    }

    // Las dos altas reciben el mismo slug libre: la segunda la frena el UNIQUE de company.slug.
    @Test
    void slugTakenByConcurrentRequestReturnsConflictAndCreatesNoAdmin() {
        when(slugGenerator.generate("Barbería Sur")).thenReturn("barberia-sur");
        when(companyRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> companyService.create(request("Barbería Sur")))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getCode()).isEqualTo("COMPANY_SLUG_TAKEN");
                });
        verify(userService, never()).createPendingAdmin(any(), anyString(), anyString(), anyString(), anyString());
    }

    private static CreateCompanyRequest request(String name) {
        return new CreateCompanyRequest(
                new CreateCompanyRequest.CompanyData(
                        name, CompanyCategory.BARBERSHOP, "America/Argentina/Cordoba", null, null, null, null, null),
                new CreateCompanyRequest.AdminData("Ana", "Gil", "ana@example.com", "+5493510000099"));
    }
}
