import { useQuery } from '@tanstack/react-query'
import { ApiRequestError, toErrorMessage } from '../api/client'
import { getPublicCompany } from '../api/publicCompany'
import type { PublicCompany, PublicService } from '../types/PublicCompany'

interface UsePublicCompanyResult {
  company: PublicCompany | null
  services: PublicService[]
  notFound: boolean
  error: string | null
  isLoading: boolean
}

// Una respuesta de error de la API no se arregla reintentando; una falla de conexión, sí.
function isApiResponseError(error: Error): boolean {
  return error instanceof ApiRequestError && error.code !== 'NETWORK_ERROR'
}

function isNotFoundError(error: Error | null): boolean {
  return error instanceof ApiRequestError && error.code === 'COMPANY_NOT_FOUND'
}

export function usePublicCompany(slug: string): UsePublicCompanyResult {
  const query = useQuery({
    queryKey: ['public-company', slug],
    queryFn: () => getPublicCompany(slug),
    retry: (failureCount, error) =>
      !isApiResponseError(error) && failureCount < 3,
  })

  return {
    company: query.data?.company ?? null,
    services: query.data?.services ?? [],
    notFound: isNotFoundError(query.error),
    error: query.error ? toErrorMessage(query.error) : null,
    isLoading: query.isPending,
  }
}
