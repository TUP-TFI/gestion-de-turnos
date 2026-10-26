package com.grupo140.turnos.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Corta con {@code 403 COMPANY_INACTIVE} toda request con sesión de una empresa desactivada (US-04.4). Sin esto,
 * un token emitido antes de la desactivación seguiría sirviendo hasta que venza.
 *
 * <p>Solo actúa si hay un {@link Jwt} autenticado con {@code companyId}, es decir, un admin de empresa o un
 * cliente. El superadmin no tiene empresa y las rutas sin sesión (login, activación y {@code /api/public/**})
 * no pasan por acá.
 *
 * <p>No es un bean: lo arma {@code SecurityConfig} y lo agrega a la cadena, así Spring Boot no lo registra
 * además como filtro del servlet.
 */
public class ActiveCompanyFilter extends OncePerRequestFilter {

    private final CompanyStatusChecker companyStatusChecker;
    private final JsonMapper jsonMapper;

    public ActiveCompanyFilter(CompanyStatusChecker companyStatusChecker, JsonMapper jsonMapper) {
        this.companyStatusChecker = companyStatusChecker;
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String companyId = jwt.getClaimAsString("companyId");
            if (companyId != null && !companyStatusChecker.isActive(UUID.fromString(companyId))) {
                ErrorResponseWriter.write(
                        response, jsonMapper, HttpStatus.FORBIDDEN, "COMPANY_INACTIVE", "La empresa está desactivada");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
