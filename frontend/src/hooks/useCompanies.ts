import { useQuery } from '@tanstack/react-query'
import { toErrorMessage } from '../api/client'
import { listCompanies } from '../api/companies'
import type { Company } from '../types/Company'

interface UseCompaniesResult {
  companies: Company[]
  error: string | null
  isLoading: boolean
}

export function useCompanies(): UseCompaniesResult {
  const query = useQuery({
    queryKey: ['companies'],
    queryFn: listCompanies,
  })

  return {
    companies: query.data ?? [],
    error: query.error ? toErrorMessage(query.error) : null,
    isLoading: query.isPending,
  }
}
