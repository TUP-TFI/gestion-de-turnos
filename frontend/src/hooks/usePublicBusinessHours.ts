import { useQuery } from '@tanstack/react-query'
import { isApiResponseError, toErrorMessage } from '../api/client'
import { getPublicBusinessHours } from '../api/publicCompany'
import type { PublicBusinessHours } from '../types/PublicCompany'

interface UsePublicBusinessHoursResult {
  hours: PublicBusinessHours[]
  error: string | null
  isLoading: boolean
}

// Solo se pide con la empresa activa: con una inactiva, el backend responde 403 COMPANY_INACTIVE.
export function usePublicBusinessHours(
  slug: string,
  enabled: boolean,
): UsePublicBusinessHoursResult {
  const query = useQuery({
    queryKey: ['public-business-hours', slug],
    queryFn: () => getPublicBusinessHours(slug),
    enabled,
    retry: (failureCount, error) =>
      !isApiResponseError(error) && failureCount < 3,
  })

  return {
    hours: query.data ?? [],
    error: query.error ? toErrorMessage(query.error) : null,
    isLoading: query.isPending,
  }
}
