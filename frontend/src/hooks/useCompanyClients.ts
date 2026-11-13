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

export function useCompanyClients(
  q: string,
  page: number,
): UseCompanyClientsResult {
  const text = q.trim()
  const query = useQuery({
    queryKey: ['company-clients', text, page],
    queryFn: () => listCompanyClients(text, page),
    // Al cambiar de página o de búsqueda se sigue viendo la tabla anterior hasta que llega la nueva, sin parpadeo.
    placeholderData: keepPreviousData,
  })

  return {
    clients: query.data ?? null,
    error: query.error ? toErrorMessage(query.error) : null,
    isLoading: query.isPending,
    isFetching: query.isFetching,
  }
}
