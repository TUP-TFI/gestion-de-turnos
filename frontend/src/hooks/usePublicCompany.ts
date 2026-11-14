import { useQuery } from '@tanstack/react-query'
import {
  ApiRequestError,
  isApiResponseError,
  toErrorMessage,
} from '../api/client'
import { getPublicCompany } from '../api/publicCompany'
import type { PublicCompany, PublicService } from '../types/PublicCompany'

interface UsePublicCompanyResult {
  company: PublicCompany | null
  services: PublicService[]
  notFound: boolean
  error: string | null
  isLoading: boolean
}

function isNotFoundError(error: Error | null): boolean {
  return error instanceof ApiRequestError && error.code === 'COMPANY_NOT_FOUND'
}

// Sin slug no consulta: el panel del admin lo recibe del branding, y llega después.
export function usePublicCompany(
  slug: string | undefined,
): UsePublicCompanyResult {
  const query = useQuery({
    queryKey: ['public-company', slug],
    queryFn: () => getPublicCompany(slug ?? ''),
    enabled: Boolean(slug),
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
