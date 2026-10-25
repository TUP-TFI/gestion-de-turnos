package com.grupo140.turnos.auth;

import static com.grupo140.turnos.seed.SeedTestValues.DEV_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.grupo140.turnos.TestcontainersConfiguration;
import com.grupo140.turnos.company.CompanyService;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro y login del cliente (US-03.5 y US-03.6) de punta a punta, con las empresas y los clientes del seed:
 * Barbería Central tiene a {@code cliente1@example.com} y Estética Pendiente no tiene clientes.
 *
 * <p>Cada test hace rollback ({@code @Transactional}): los clientes registrados no quedan guardados.
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ClientAuthIT {

    private static final String BARBERIA = "barberia-central";
    private static final String ESTETICA = "estetica-pendiente";
    private static final String CLIENT_EMAIL = "cliente1@example.com";
    private static final String BODY_WITHOUT_PASSWORD =
            "{\"firstName\":\"Ana\",\"lastName\":\"Gil\",\"email\":\"a@example.com\",\"phone\":\"+5493510000099\"}";
    private static final String BODY_WITHOUT_PHONE =
            "{\"firstName\":\"Ana\",\"lastName\":\"Gil\",\"email\":\"a@example.com\",\"password\":\"Clave.nueva.26\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyService companyService;

    @Test
    void registerCreatesClientAndLogsIn() throws Exception {
        String body = mockMvc.perform(register(BARBERIA, "nuevo@example.com", "Clave.nueva.2026"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value("nuevo@example.com"))
                .andExpect(jsonPath("$.user.role").value("CLIENT"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // El token trae el rol y la empresa, y la audiencia del cliente.
        String claims = payloadOf(JsonPath.read(body, "$.token"));
        UUID companyId = companyService.requireActiveIdBySlug(BARBERIA);
        assertThat((String) JsonPath.read(claims, "$.role")).isEqualTo("CLIENT");
        assertThat((String) JsonPath.read(claims, "$.companyId")).isEqualTo(companyId.toString());
        // Con una sola audiencia, el JWT la guarda como texto y no como lista.
        assertThat((String) JsonPath.read(claims, "$.aud")).isEqualTo("client");

        // Y la sesión sirve: queda logueado sin pasar por el login.
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + JsonPath.read(body, "$.token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CLIENT"));
        mockMvc.perform(login(BARBERIA, "nuevo@example.com", "Clave.nueva.2026"))
                .andExpect(status().isOk());
    }

    @Test
    void duplicateEmailInSameCompanyReturns409() throws Exception {
        mockMvc.perform(register(BARBERIA, CLIENT_EMAIL, "Clave.nueva.2026"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_IN_USE"));
        // El email no distingue mayúsculas.
        mockMvc.perform(register(BARBERIA, "CLIENTE1@EXAMPLE.COM", "Clave.nueva.2026"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_IN_USE"));
        // El email de un admin de la misma empresa tampoco se puede usar.
        mockMvc.perform(register(BARBERIA, "admin@example.com", "Clave.nueva.2026"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_IN_USE"));
    }

    @Test
    void sameEmailInAnotherCompanyIsAllowed() throws Exception {
        mockMvc.perform(register(ESTETICA, CLIENT_EMAIL, "Clave.nueva.2026")).andExpect(status().isCreated());
    }

    @Test
    void invalidBodyReturns400() throws Exception {
        mockMvc.perform(register(BARBERIA, "corta@example.com", "corta"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(post(path(BARBERIA, "register"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_WITHOUT_PHONE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(post(path(BARBERIA, "register"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_WITHOUT_PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void loginSucceedsWithSeedClient() throws Exception {
        mockMvc.perform(login(BARBERIA, CLIENT_EMAIL, DEV_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("CLIENT"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        mockMvc.perform(login(BARBERIA, CLIENT_EMAIL, "Otra.clave.2026"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void clientOfAnotherCompanyCannotLogIn() throws Exception {
        mockMvc.perform(login(ESTETICA, CLIENT_EMAIL, DEV_PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void companyAdminCannotUseClientLogin() throws Exception {
        mockMvc.perform(login(BARBERIA, "admin@example.com", DEV_PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void clientCannotUsePanelLogin() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(CLIENT_EMAIL, DEV_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void unknownSlugReturns404() throws Exception {
        mockMvc.perform(register("no-existe", "nuevo@example.com", "Clave.nueva.2026"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPANY_NOT_FOUND"));
        mockMvc.perform(login("no-existe", CLIENT_EMAIL, DEV_PASSWORD))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPANY_NOT_FOUND"));
    }

    @Test
    void inactiveCompanyReturns403() throws Exception {
        companyService.deactivate(companyService.requireActiveIdBySlug(BARBERIA));

        mockMvc.perform(register(BARBERIA, "nuevo@example.com", "Clave.nueva.2026"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMPANY_INACTIVE"));
        mockMvc.perform(login(BARBERIA, CLIENT_EMAIL, DEV_PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMPANY_INACTIVE"));
    }

    @Test
    void clientTokenCannotReachCompanyRoutes() throws Exception {
        mockMvc.perform(get("/api/company/services").header("Authorization", "Bearer " + clientToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    // #54: un token emitido antes de desactivar la empresa deja de servir en el acto.
    @Test
    void openSessionIsCutWhenCompanyIsDeactivated() throws Exception {
        String token = clientToken();
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        companyService.deactivate(companyService.requireActiveIdBySlug(BARBERIA));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMPANY_INACTIVE"));
    }

    private String clientToken() throws Exception {
        String body = mockMvc.perform(login(BARBERIA, CLIENT_EMAIL, DEV_PASSWORD))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private static String path(String slug, String action) {
        return "/api/public/companies/" + slug + "/" + action;
    }

    private static MockHttpServletRequestBuilder register(String slug, String email, String password) {
        return post(path(slug, "register"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(email, password));
    }

    private static String registerBody(String email, String password) {
        String json = "{\"firstName\":\"Ana\",\"lastName\":\"Gil\",\"email\":\"%s\","
                + "\"phone\":\"+5493510000099\",\"password\":\"%s\"}";
        return json.formatted(email, password);
    }

    private static MockHttpServletRequestBuilder login(String slug, String email, String password) {
        return post(path(slug, "login")).contentType(MediaType.APPLICATION_JSON).content(credentials(email, password));
    }

    private static String credentials(String email, String password) {
        return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
    }

    // El payload de un JWT es JSON en Base64 URL, entre los dos puntos.
    private static String payloadOf(String token) {
        return new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
    }
}
