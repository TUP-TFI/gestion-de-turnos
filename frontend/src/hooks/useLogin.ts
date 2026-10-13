import { useMutation } from '@tanstack/react-query'
import { login } from '../api/auth'
import { toErrorMessage } from '../api/client'
import type { LoginRequest } from '../types/Auth'
import { useAuth } from './useAuth'

interface UseLoginResult {
  login: (request: LoginRequest) => void
  error: string | null
  isLoading: boolean
}

export function useLogin(): UseLoginResult {
  const { startSession } = useAuth()

  const mutation = useMutation({
    mutationFn: login,
    onSuccess: startSession,
  })

  return {
    login: mutation.mutate,
    error: mutation.error ? toErrorMessage(mutation.error) : null,
    isLoading: mutation.isPending,
  }
}
