import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toErrorMessage } from '../api/client'
import { updatePlatformSettings } from '../api/settings'
import type { PlatformSettings, PlatformSettingsData } from '../types/PlatformSettings'

interface UseUpdatePlatformSettingsResult {
  updateSettings: (body: PlatformSettingsData) => void
  updatedSettings: PlatformSettings | null
  error: string | null
  isLoading: boolean
  reset: () => void
}

export function useUpdatePlatformSettings(): UseUpdatePlatformSettingsResult {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: updatePlatformSettings,
    // La pantalla no se recarga al guardar: se deja en la caché lo que respondió el back, sin pedirlo de nuevo.
    onSuccess: (saved) => queryClient.setQueryData(['settings'], saved),
  })

  return {
    updateSettings: mutation.mutate,
    updatedSettings: mutation.data ?? null,
    error: mutation.error ? toErrorMessage(mutation.error) : null,
    isLoading: mutation.isPending,
    reset: mutation.reset,
  }
}
