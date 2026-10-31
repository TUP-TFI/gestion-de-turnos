package com.grupo140.turnos.schedule;

import static com.grupo140.turnos.seed.SeedTestValues.DEV_PASSWORD;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.grupo140.turnos.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Horario de atención de la página pública de punta a punta, sin login, con el horario del seed de Barbería
 * Central: lunes a viernes de 9 a 13 y de 16 a 20, sábado de 9 a 13 y domingo cerrado. Estética Pendiente no tiene
 * horario.
 *
 * <p>Cada test hace rollback ({@code @Transactional}): los cambios no pasan al test siguiente.
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class PublicBusinessHoursIT {

    private static final String PUBLIC = "/api/public/companies/";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsWeeklyHoursInOrderWithSplitShiftWithoutLogin() throws Exception {
        // 5 días con dos franjas, más el sábado.
        mockMvc.perform(get(PUBLIC + "barberia-central/business-hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(11))
                .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$[0].startTime", startsWith("09:00")))
                .andExpect(jsonPath("$[0].endTime", startsWith("13:00")))
                .andExpect(jsonPath("$[1].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$[1].startTime", startsWith("16:00")))
                .andExpect(jsonPath("$[1].endTime", startsWith("20:00")))
                .andExpect(jsonPath("$[10].dayOfWeek").value("SATURDAY"))
                .andExpect(jsonPath("$[10].startTime", startsWith("09:00")));
    }

    @Test
    void closedDayHasNoRows() throws Exception {
        mockMvc.perform(get(PUBLIC + "barberia-central/business-hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.dayOfWeek == 'SUNDAY')]").isEmpty());
    }

    @Test
    void responseDoesNotExposeInternalFields() throws Exception {
        mockMvc.perform(get(PUBLIC + "barberia-central/business-hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").doesNotExist())
                .andExpect(jsonPath("$[0].companyId").doesNotExist())
                .andExpect(jsonPath("$[0].intervalMinutes").doesNotExist())
                .andExpect(jsonPath("$[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$[0].updatedAt").doesNotExist());
    }

    @Test
    void companyWithoutHoursReturnsEmptyList() throws Exception {
        mockMvc.perform(get(PUBLIC + "estetica-pendiente/business-hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void unknownSlugReturns404() throws Exception {
        mockMvc.perform(get(PUBLIC + "no-existe/business-hours"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPANY_NOT_FOUND"));
    }

    @Test
    void inactiveCompanyReturns403() throws Exception {
        String token = superadminToken();
        String companies = mockMvc.perform(get("/api/superadmin/companies").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> ids = JsonPath.read(companies, "$[?(@.slug == 'barberia-central')].id");
        mockMvc.perform(post("/api/superadmin/companies/" + ids.getFirst() + "/deactivate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get(PUBLIC + "barberia-central/business-hours"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMPANY_INACTIVE"));
    }

    private String superadminToken() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"superadmin@example.com\",\"password\":\"%s\"}".formatted(DEV_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.token");
    }
}
