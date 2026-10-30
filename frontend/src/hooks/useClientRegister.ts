import { useMutation } from '@tanstack/react-query'
import { registerClient } from '../api/clientAuth'
import { toErrorMessage } from '../api/client'
import type { ClientRegisterRequest } from '../types/Auth'
import { useClientAuth } from './useClientAuth'

interface UseClientRegisterResult {
  register: (request: ClientRegisterRequest) => void
  error: string | null
  isLoading: boolean
}

// Al registrarse, el cliente queda con la sesión iniciada: el back responde igual que el login.
export function useClientRegister(slug: string): UseClientRegisterResult {
  const { startSession } = useClientAuth(slug)

  const mutation = useMutation({
    mutationFn: (request: ClientRegisterRequest) =>
      registerClient(slug, request),
    onSuccess: (response) => startSession({ ...response, slug }),
  })

  return {
    register: mutation.mutate,
    error: mutation.error ? toErrorMessage(mutation.error) : null,
    isLoading: mutation.isPending,
  }
}
