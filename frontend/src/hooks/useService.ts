import { useQuery } from '@tanstack/react-query'
import { ApiRequestError, toErrorMessage } from '../api/client'
import { getService } from '../api/services'
import type { Service } from '../types/Service'

interface UseServiceResult {
  service: Service | null
  error: string | null
  isLoading: boolean
  isNotFound: boolean
}

// VALIDATION_ERROR solo llega cuando el id de la URL no es un UUID: tampoco existe.
function isNotFoundError(error: Error | null): boolean {
  return (
    error instanceof ApiRequestError &&
    (error.code === 'SERVICE_NOT_FOUND' || error.code === 'VALIDATION_ERROR')
  )
}

export function useService(id: string): UseServiceResult {
  const query = useQuery({
    queryKey: ['services', id],
    queryFn: () => getService(id),
    // Un 404 no se arregla reintentando.
    retry: (failureCount, error) => !isNotFoundError(error) && failureCount < 3,
  })

  return {
    service: query.data ?? null,
    error: query.error ? toErrorMessage(query.error) : null,
    isLoading: query.isPending,
    isNotFound: isNotFoundError(query.error),
  }
}
