import { useQuery } from '@tanstack/react-query'
import { ApiRequestError, toErrorMessage } from '../api/client'
import { getCompany } from '../api/companies'
import type { Company } from '../types/Company'

interface UseCompanyResult {
  company: Company | null
  error: string | null
  isLoading: boolean
  isNotFound: boolean
}

// NOT_FOUND llega cuando el id de la URL ni siquiera es un UUID.
function isNotFoundError(error: Error | null): boolean {
  return (
    error instanceof ApiRequestError &&
    (error.code === 'COMPANY_NOT_FOUND' || error.code === 'NOT_FOUND')
  )
}

export function useCompany(id: string): UseCompanyResult {
  const query = useQuery({
    queryKey: ['companies', id],
    queryFn: () => getCompany(id),
    // Un 404 no se arregla reintentando.
    retry: (failureCount, error) => !isNotFoundError(error) && failureCount < 3,
  })

  return {
    company: query.data ?? null,
    error: query.error ? toErrorMessage(query.error) : null,
    isLoading: query.isPending,
    isNotFound: isNotFoundError(query.error),
  }
}
