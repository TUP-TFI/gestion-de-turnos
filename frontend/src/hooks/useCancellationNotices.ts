import { useQuery } from '@tanstack/react-query'
import { isApiResponseError, toErrorMessage } from '../api/client'
import { getCancellationNotices } from '../api/companies'
import type { CancellationNotices } from '../types/CancellationNotice'

interface UseCancellationNoticesResult {
  data: CancellationNotices | null
  error: string | null
  isLoading: boolean
}

export function useCancellationNotices(
  id: string,
): UseCancellationNoticesResult {
  const query = useQuery({
    // Cuelga de ['companies', id]: cualquier cambio de la empresa la invalida.
    queryKey: ['companies', id, 'cancellation-notices'],
    queryFn: () => getCancellationNotices(id),
    // Una respuesta de error de la API, como COMPANY_NOT_FOUND, no se arregla reintentando.
    retry: (failureCount, error) =>
      !isApiResponseError(error) && failureCount < 3,
  })

  return {
    data: query.data ?? null,
    error: query.error ? toErrorMessage(query.error) : null,
    isLoading: query.isPending,
  }
}
