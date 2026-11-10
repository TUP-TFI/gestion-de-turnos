// Forma común de las respuestas paginadas de la API (PageResponse del backend).
export interface PageResponse<T> {
  content: T[]
  // Número de página, empezando en 0.
  page: number
  // Cantidad máxima de elementos por página.
  size: number
  // Cantidad de elementos en todas las páginas.
  totalElements: number
  totalPages: number
}
