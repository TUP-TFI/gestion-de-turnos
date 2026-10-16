import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toErrorMessage } from '../api/client'
import { createCompany } from '../api/companies'
import type { Company, CreateCompanyRequest } from '../types/Company'

interface UseCreateCompanyResult {
  createCompany: (request: CreateCompanyRequest) => void
  createdCompany: Company | null
  error: string | null
  isLoading: boolean
  reset: () => void
}

export function useCreateCompany(): UseCreateCompanyResult {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: createCompany,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['companies'] }),
  })

  return {
    createCompany: mutation.mutate,
    createdCompany: mutation.data ?? null,
    error: mutation.error ? toErrorMessage(mutation.error) : null,
    isLoading: mutation.isPending,
    reset: mutation.reset,
  }
}
