import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toErrorMessage } from '../api/client'
import { resendActivation } from '../api/companies'

interface UseResendActivationResult {
  resend: () => void
  sent: boolean
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
    sent: mutation.isSuccess,
    error: mutation.error ? toErrorMessage(mutation.error) : null,
    isLoading: mutation.isPending,
  }
}
