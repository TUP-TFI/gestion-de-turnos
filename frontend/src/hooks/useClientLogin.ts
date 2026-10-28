import { useMutation } from '@tanstack/react-query'
import { loginClient } from '../api/clientAuth'
import { toErrorMessage } from '../api/client'
import type { LoginRequest } from '../types/Auth'
import { useClientAuth } from './useClientAuth'

interface UseClientLoginResult {
  login: (request: LoginRequest) => void
  error: string | null
  isLoading: boolean
}

export function useClientLogin(slug: string): UseClientLoginResult {
  const { startSession } = useClientAuth(slug)

  const mutation = useMutation({
    mutationFn: (request: LoginRequest) => loginClient(slug, request),
    // El back no devuelve el slug: se guarda el de la URL, que es el de la empresa donde entró.
    onSuccess: (response) => startSession({ ...response, slug }),
  })

  return {
    login: mutation.mutate,
    error: mutation.error ? toErrorMessage(mutation.error) : null,
    isLoading: mutation.isPending,
  }
}
