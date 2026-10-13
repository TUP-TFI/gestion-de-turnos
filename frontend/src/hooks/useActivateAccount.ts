import { useMutation } from '@tanstack/react-query'
import { activateAccount } from '../api/auth'
import { toErrorMessage } from '../api/client'
import type { ActivationRequest } from '../types/Auth'

interface UseActivateAccountResult {
  activateAccount: (request: ActivationRequest) => void
  error: string | null
  isLoading: boolean
  isActivated: boolean
}

export function useActivateAccount(): UseActivateAccountResult {
  const mutation = useMutation({ mutationFn: activateAccount })

  return {
    activateAccount: mutation.mutate,
    error: mutation.error ? toErrorMessage(mutation.error) : null,
    isLoading: mutation.isPending,
    isActivated: mutation.isSuccess,
  }
}
