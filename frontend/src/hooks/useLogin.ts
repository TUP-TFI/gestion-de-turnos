import { useMutation } from '@tanstack/react-query'
import { login } from '../api/auth'
import { ApiRequestError } from '../api/client'
import type { LoginRequest } from '../types/Auth'
import { useAuth } from './useAuth'

const UNEXPECTED_ERROR = 'Ocurrió un error inesperado. Intentá de nuevo.'

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
    error: mutation.error ? toMessage(mutation.error) : null,
    isLoading: mutation.isPending,
  }
}

function toMessage(error: Error): string {
  return error instanceof ApiRequestError ? error.message : UNEXPECTED_ERROR
}
