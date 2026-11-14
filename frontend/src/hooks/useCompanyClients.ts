import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { toErrorMessage } from '../api/client'
import { listCompanyClients } from '../api/companyClients'
import type { CompanyClient } from '../types/Client'
import type { PageResponse } from '../types/Page'

interface UseCompanyClientsResult {
  clients: PageResponse<CompanyClient> | null
  error: string | null
  isLoading: boolean
  // Hay una consulta en curso, también cuando ya se ve la tabla anterior (cambio de página o de búsqueda).
  isFetching: boolean
}

interface UseCompanyClientsOptions {
  // Clientes por página. Sin valor, el del backend.
  size?: number
  // Con false no se consulta: por ejemplo, un buscador que no muestra nada mientras no se escribió nada.
  enabled?: boolean
}

export function useCompanyClients(
  q: string,
  page: number,
  { size, enabled = true }: UseCompanyClientsOptions = {},
): UseCompanyClientsResult {
  const text = q.trim()
  const query = useQuery({
    queryKey: ['company-clients', text, page, size],
    queryFn: () => listCompanyClients(text, page, size),
    enabled,
    // Al cambiar de página o de búsqueda se sigue viendo la tabla anterior hasta que llega la nueva, sin parpadeo.
    placeholderData: keepPreviousData,
  })

  return {
    clients: query.data ?? null,
    error: query.error ? toErrorMessage(query.error) : null,
    // isLoading y no isPending: una consulta apagada no tiene datos, pero tampoco está cargando.
    isLoading: query.isLoading,
    isFetching: query.isFetching,
  }
}
