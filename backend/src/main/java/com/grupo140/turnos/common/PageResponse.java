package com.grupo140.turnos.common;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Forma común de las respuestas paginadas de la API. Es un DTO propio para que el JSON no dependa del {@code Page} de
 * Spring.
 *
 * @param page número de página, empezando en 0.
 * @param size cantidad máxima de elementos por página.
 * @param totalElements cantidad de elementos en todas las páginas.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
