import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateBranding } from '../api/branding'
import { toErrorMessage } from '../api/client'
import type { CompanyBrandingData } from '../types/CompanyBranding'

interface UseUpdateBrandingResult {
  updateBranding: (body: CompanyBrandingData) => void
  saved: boolean
  error: string | null
  isLoading: boolean
  reset: () => void
}

export function useUpdateBranding(): UseUpdateBrandingResult {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: updateBranding,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['branding'] }),
  })

  return {
    updateBranding: mutation.mutate,
    saved: mutation.isSuccess,
    error: mutation.error ? toErrorMessage(mutation.error) : null,
    isLoading: mutation.isPending,
    reset: mutation.reset,
  }
}
