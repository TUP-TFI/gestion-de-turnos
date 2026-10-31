import { useQuery } from '@tanstack/react-query'
import { toErrorMessage } from '../api/client'
import { getPublicCompany } from '../api/publicCompany'
import type { PublicCompany, PublicService } from '../types/PublicCompany'

interface UsePublicCompanyResult {
  company: PublicCompany | null
  services: PublicService[]
  error: string | null
  isLoading: boolean
}

export function usePublicCompany(slug: string): UsePublicCompanyResult {
  const query = useQuery({
    queryKey: ['public-company', slug],
    queryFn: () => getPublicCompany(slug),
  })

  return {
    company: query.data?.company ?? null,
    services: query.data?.services ?? [],
    error: query.error ? toErrorMessage(query.error) : null,
    isLoading: query.isPending,
  }
}
