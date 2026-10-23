import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toErrorMessage } from '../api/client'
import { updateService } from '../api/services'
import type { Service, ServiceData } from '../types/Service'

interface UseUpdateServiceResult {
  updateService: (body: ServiceData) => void
  updatedService: Service | null
  error: string | null
  isLoading: boolean
  reset: () => void
}

export function useUpdateService(id: string): UseUpdateServiceResult {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: (body: ServiceData) => updateService(id, body),
    // Invalida el listado y también el detalle ['services', id].
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['services'] }),
  })

  return {
    updateService: mutation.mutate,
    updatedService: mutation.data ?? null,
    error: mutation.error ? toErrorMessage(mutation.error) : null,
    isLoading: mutation.isPending,
    reset: mutation.reset,
  }
}
