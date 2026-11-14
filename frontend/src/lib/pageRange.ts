import type { PageResponse } from '../types/Page'

// Total de una lista paginada para mostrar sobre la tabla. Con una sola página, "N turnos"; con más, qué tramo se ve:
// "21–40 de 45 turnos". Sale de la página, el tamaño y el total de la respuesta.
export function formatPageTotal(
  result: PageResponse<unknown>,
  singular: string,
  plural: string,
): string {
  const total = result.totalElements
  const noun = total === 1 ? singular : plural
  if (result.totalPages <= 1) {
    return `${total} ${noun}`
  }
  const from = result.page * result.size + 1
  const to = Math.min((result.page + 1) * result.size, total)
  return `${from}–${to} de ${total} ${noun}`
}
