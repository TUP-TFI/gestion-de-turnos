import { MUTED_TEXT_CLASSES, SECONDARY_BUTTON_CLASSES } from '../lib/formStyles'

interface PaginationProps {
  // Página actual, empezando en 0.
  page: number
  totalPages: number
  onChange: (page: number) => void
}

// Anterior, Siguiente y "Página N de M". Con una sola página no se muestra.
export function Pagination({ page, totalPages, onChange }: PaginationProps) {
  if (totalPages <= 1) {
    return null
  }

  return (
    <nav
      aria-label="Paginado"
      className="flex flex-wrap items-center justify-between gap-3"
    >
      <button
        type="button"
        onClick={() => onChange(page - 1)}
        disabled={page <= 0}
        className={SECONDARY_BUTTON_CLASSES}
      >
        Anterior
      </button>
      <p className={MUTED_TEXT_CLASSES} aria-live="polite">
        Página {page + 1} de {totalPages}
      </p>
      <button
        type="button"
        onClick={() => onChange(page + 1)}
        disabled={page >= totalPages - 1}
        className={SECONDARY_BUTTON_CLASSES}
      >
        Siguiente
      </button>
    </nav>
  )
}
