package com.grupo140.turnos.user;

import com.grupo140.turnos.common.PageResponse;
import com.grupo140.turnos.common.error.ApiException;
import com.grupo140.turnos.common.security.SessionUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Clientes de la empresa, de solo lectura. El prefijo {@code /api/company} ya exige el rol {@code COMPANY_ADMIN} (ver
 * {@code SecurityConfig}), y la empresa sale siempre de la sesión.
 */
@RestController
@RequestMapping("/api/company/clients")
public class CompanyClientController {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserService userService;

    public CompanyClientController(UserService userService) {
        this.userService = userService;
    }

    /** Listado de clientes (US-09.7). Con {@code q}, busca por nombre, teléfono o email (US-09.4). */
    @GetMapping
    public PageResponse<ClientResponse> list(
            SessionUser session,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "La página o el tamaño pedidos no son válidos");
        }
        return userService.searchClients(session.companyId(), q, page, size);
    }
}
