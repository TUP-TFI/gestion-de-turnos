import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router'
import { Pagination } from '../../components/Pagination'
import { useCompanyClients } from '../../hooks/useCompanyClients'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'
import { usePageInRange } from '../../hooks/usePageInRange'
import { formatPageTotal } from '../../lib/pageRange'
import { telHref } from '../../lib/phoneLink'
import {
  ERROR_CLASSES,
  INPUT_CLASSES,
  LABEL_CLASSES,
  LINK_CLASSES,
  MUTED_TEXT_CLASSES,
  PAGE_TITLE_CLASSES,
  SECONDARY_BUTTON_CLASSES,
  TABLE_CELL_CLASSES,
  TABLE_HEAD_CLASSES,
  TABLE_WRAPPER_CLASSES,
} from '../../lib/formStyles'

const SEARCH_DELAY_MS = 300

// La búsqueda y la página viven en la URL: se pueden recargar y compartir, y "volver" respeta la búsqueda.
function readQuery(params: URLSearchParams): string {
  return params.get('q')?.trim() ?? ''
}

function readPage(params: URLSearchParams): number {
  const page = Number(params.get('page'))
  return Number.isInteger(page) && page > 0 ? page : 0
}

// Clientes registrados en la empresa, solo lectura (US-09.7).
export function ClientsPage() {
  const [params, setParams] = useSearchParams()
  const q = readQuery(params)
  const page = readPage(params)
  // Lo que se escribe se ve al instante; a la URL y al back llega 300 ms después de dejar de escribir.
  const [text, setText] = useState(q)
  const debouncedText = useDebouncedValue(text, SEARCH_DELAY_MS)
  const { clients, error, isLoading, isFetching } = useCompanyClients(q, page)
  // Corrige la URL si pide una página que ya no existe.
  const isOutOfRange = usePageInRange(clients, page)

  // Si la URL cambia por fuera de lo que se escribió (por ejemplo con "volver"), el input muestra la de la URL.
  const [previousQ, setPreviousQ] = useState(q)
  if (q !== previousQ) {
    setPreviousQ(q)
    if (q !== debouncedText.trim()) {
      setText(q)
    }
  }

  // Cuando se deja de escribir, la búsqueda pasa a la URL y vuelve a la primera página. Se reemplaza la URL, sin sumar
  // una entrada al historial por cada pausa. Mientras el texto y su versión con demora no coinciden, se sigue
  // escribiendo (o la URL acaba de cambiar por fuera) y no se toca nada. Sin texto, la búsqueda sale de la URL.
  useEffect(() => {
    const value = debouncedText.trim()
    if (text.trim() !== value || value === q) {
      return
    }
    const next = new URLSearchParams(params)
    if (value) {
      next.set('q', value)
    } else {
      next.delete('q')
    }
    next.delete('page')
    setParams(next, { replace: true })
  }, [text, debouncedText, q, params, setParams])

  function clearSearch() {
    setText('')
    const next = new URLSearchParams(params)
    next.delete('q')
    next.delete('page')
    setParams(next, { replace: true })
  }

  function changePage(newPage: number) {
    const next = new URLSearchParams(params)
    if (newPage > 0) {
      next.set('page', String(newPage))
    } else {
      next.delete('page')
    }
    setParams(next)
  }

  const rows = clients?.content ?? []
  // Se escribió algo que todavía no llegó a la URL: la tabla es de la búsqueda anterior.
  const isSearching = text.trim() !== q
  const isRefreshing = isFetching && !isLoading
  const isStale = isSearching || isRefreshing
  const showResults = clients !== null && !error && !isOutOfRange

  return (
    <section className="space-y-6">
      <div className="space-y-1">
        <h2 className={PAGE_TITLE_CLASSES}>Clientes</h2>
        <p className={MUTED_TEXT_CLASSES}>
          Son las cuentas que crearon los clientes al registrarse. No se editan
          desde acá.
        </p>
      </div>

      <div className="space-y-1">
        <label htmlFor="client-search" className={LABEL_CLASSES}>
          Buscar por nombre, email o teléfono
        </label>
        <div className="flex flex-wrap gap-2">
          <input
            id="client-search"
            type="text"
            enterKeyHint="search"
            value={text}
            onChange={(event) => setText(event.target.value)}
            autoComplete="off"
            className={`${INPUT_CLASSES} min-w-0 flex-1`}
          />
          {text && (
            <button
              type="button"
              onClick={clearSearch}
              className={SECONDARY_BUTTON_CLASSES}
            >
              Limpiar
            </button>
          )}
        </div>
        <p className={MUTED_TEXT_CLASSES}>
          El nombre se busca sin importar mayúsculas ni tildes; el email, sin
          importar mayúsculas, y el teléfono, tal como lo cargó el cliente.
        </p>
      </div>

      {(isLoading || isOutOfRange) && (
        <p className={MUTED_TEXT_CLASSES}>Cargando...</p>
      )}

      {error && (
        <p role="alert" className={ERROR_CLASSES}>
          {error}
        </p>
      )}

      {clients && showResults && (
        <div className="flex flex-wrap items-center justify-between gap-2">
          <p className={MUTED_TEXT_CLASSES}>
            {formatPageTotal(clients, 'cliente', 'clientes')}
          </p>
          <p className={MUTED_TEXT_CLASSES} aria-live="polite">
            {isSearching ? 'Buscando...' : isRefreshing ? 'Actualizando...' : ''}
          </p>
        </div>
      )}

      {!isLoading && !error && !isOutOfRange && rows.length === 0 && (
        <div className="space-y-3 text-center">
          <p className={MUTED_TEXT_CLASSES}>
            {q
              ? `No hay clientes que coincidan con "${q}"`
              : 'Todavía no hay clientes registrados'}
          </p>
          {q && (
            <button
              type="button"
              onClick={clearSearch}
              className={SECONDARY_BUTTON_CLASSES}
            >
              Limpiar búsqueda
            </button>
          )}
        </div>
      )}

      {rows.length > 0 && !isOutOfRange && (
        <div
          aria-busy={isStale}
          className={`${TABLE_WRAPPER_CLASSES} transition-opacity ${isStale ? 'opacity-60' : ''}`}
        >
          <table className="w-full text-left text-sm text-body">
            <thead className={TABLE_HEAD_CLASSES}>
              <tr>
                <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                  Nombre
                </th>
                <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                  Email
                </th>
                <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                  Teléfono
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-hairline">
              {rows.map((client) => (
                <tr key={client.id}>
                  <td className={`${TABLE_CELL_CLASSES} font-medium text-ink`}>
                    {client.lastName}, {client.firstName}
                  </td>
                  <td className={TABLE_CELL_CLASSES}>
                    <a href={`mailto:${client.email}`} className={LINK_CLASSES}>
                      {client.email}
                    </a>
                  </td>
                  <td className={`${TABLE_CELL_CLASSES} whitespace-nowrap`}>
                    <a
                      href={telHref(client.phone)}
                      className={LINK_CLASSES}
                    >
                      {client.phone}
                    </a>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {clients && !isOutOfRange && (
        <Pagination
          page={clients.page}
          totalPages={clients.totalPages}
          onChange={changePage}
        />
      )}
    </section>
  )
}
