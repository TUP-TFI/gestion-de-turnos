import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toErrorMessage } from '../api/client'
import { updateCompany } from '../api/companies'
import type { Company, CompanyData } from '../types/Company'

interface UseUpdateCompanyResult {
  updateCompany: (body: CompanyData) => void
  updatedCompany: Company | null
  error: string | null
  isLoading: boolean
  reset: () => void
}

export function useUpdateCompany(id: string): UseUpdateCompanyResult {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: (body: CompanyData) => updateCompany(id, body),
    // Invalida el listado y también el detalle ['companies', id].
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['companies'] }),
  })

  return {
    updateCompany: mutation.mutate,
    updatedCompany: mutation.data ?? null,
    error: mutation.error ? toErrorMessage(mutation.error) : null,
    isLoading: mutation.isPending,
    reset: mutation.reset,
  }
}
