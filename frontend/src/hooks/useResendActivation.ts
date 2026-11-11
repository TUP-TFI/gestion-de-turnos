import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toErrorMessage } from '../api/client'
import { resendActivation } from '../api/companies'
import type { ActivationLink } from '../types/Company'

interface UseResendActivationResult {
  resend: () => void
  // El link nuevo y el teléfono del admin, o null si todavía no se generó.
  activation: ActivationLink | null
  error: string | null
  isLoading: boolean
}

export function useResendActivation(id: string): UseResendActivationResult {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: () => resendActivation(id),
    // Refresca el detalle: si el admin ya había activado, el estado cambió.
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['companies', id] }),
  })

  return {
    resend: mutation.mutate,
    activation: mutation.data ?? null,
    error: mutation.error ? toErrorMessage(mutation.error) : null,
    isLoading: mutation.isPending,
  }
}
