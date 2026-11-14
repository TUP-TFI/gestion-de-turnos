import { useState } from 'react'
import { useCompanyClients } from '../hooks/useCompanyClients'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import {
  ERROR_CLASSES,
  INPUT_CLASSES,
  LABEL_CLASSES,
  LIST_OPTION_BUTTON_CLASSES,
  MUTED_TEXT_CLASSES,
} from '../lib/formStyles'
import type { CompanyClient } from '../types/Client'

const SEARCH_DELAY_MS = 300
// Pocos resultados: es una lista para elegir, no un listado. Si no entra, se escribe más para acotar.
const RESULTS_SIZE = 8

interface ClientSearchProps {
  onSelect: (client: CompanyClient) => void
  // Enfoca el input al montarse. Solo para cuando el usuario llegó acá a propósito ("Cambiar"): al cargar la página el
  // foco no se mueve.
  autoFocus?: boolean
}

// Busca entre los clientes de la empresa por nombre, email o teléfono (US-09.4) y deja elegir uno de una lista de
// botones, que se recorre con Tab. No es un combobox ARIA: el resultado vive aparte del input. Sin texto no muestra
// nada. Quien lo usa guarda al elegido y deja de mostrarlo.
export function ClientSearch({ onSelect, autoFocus }: ClientSearchProps) {
  const [text, setText] = useState('')
  const query = useDebouncedValue(text, SEARCH_DELAY_MS).trim()
  const { clients, error, isLoading, isFetching } = useCompanyClients(
    query,
    0,
    { size: RESULTS_SIZE, enabled: query !== '' },
  )

  // Se escribió algo que todavía no se consultó, o la consulta está en curso.
  const isSearching = text.trim() !== query || isLoading || isFetching
  const results = query ? (clients?.content ?? []) : []

  return (
    <div className="space-y-2">
      <div>
        <label htmlFor="client-search-input" className={LABEL_CLASSES}>
          Buscar por nombre, email o teléfono
        </label>
        <input
          id="client-search-input"
          type="text"
          enterKeyHint="search"
          value={text}
          onChange={(event) => setText(event.target.value)}
          autoComplete="off"
          autoFocus={autoFocus}
          className={INPUT_CLASSES}
        />
      </div>

      <div aria-live="polite" className="space-y-2">
        {text.trim() !== '' && isSearching && (
          <p className={MUTED_TEXT_CLASSES}>Buscando...</p>
        )}
        {query && error && (
          <p role="alert" className={ERROR_CLASSES}>
            {error}
          </p>
        )}
        {query && !isSearching && !error && results.length === 0 && (
          <p className={MUTED_TEXT_CLASSES}>No hay clientes que coincidan</p>
        )}
      </div>

      {results.length > 0 && (
        <ul
          aria-busy={isSearching}
          className={`space-y-2 transition-opacity ${isSearching ? 'opacity-60' : ''}`}
        >
          {results.map((client) => (
            <li key={client.id}>
              <button
                type="button"
                onClick={() => onSelect(client)}
                className={LIST_OPTION_BUTTON_CLASSES}
              >
                <span className="font-medium">
                  {client.lastName}, {client.firstName}
                </span>
                <span className={MUTED_TEXT_CLASSES}>
                  {client.email} · {client.phone}
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}

      {clients && clients.totalElements > results.length && results.length > 0 && (
        <p className={MUTED_TEXT_CLASSES}>
          Se muestran {results.length} de {clients.totalElements}. Escribí más
          para acotar la búsqueda.
        </p>
      )}
    </div>
  )
}
