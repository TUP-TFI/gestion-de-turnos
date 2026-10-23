import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toErrorMessage } from '../api/client'
import { createService } from '../api/services'
import type { Service, ServiceData } from '../types/Service'

interface UseCreateServiceResult {
  createService: (body: ServiceData) => void
  createdService: Service | null
  error: string | null
  isLoading: boolean
  reset: () => void
}

export function useCreateService(): UseCreateServiceResult {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: createService,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['services'] }),
  })

  return {
    createService: mutation.mutate,
    createdService: mutation.data ?? null,
    error: mutation.error ? toErrorMessage(mutation.error) : null,
    isLoading: mutation.isPending,
    reset: mutation.reset,
  }
}
