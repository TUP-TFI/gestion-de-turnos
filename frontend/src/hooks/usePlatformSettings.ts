import { useQuery } from '@tanstack/react-query'
import { toErrorMessage } from '../api/client'
import { getPlatformSettings } from '../api/settings'
import type { PlatformSettings } from '../types/PlatformSettings'

interface UsePlatformSettingsResult {
  settings: PlatformSettings | null
  error: string | null
  isLoading: boolean
}

export function usePlatformSettings(): UsePlatformSettingsResult {
  const query = useQuery({
    queryKey: ['settings'],
    queryFn: getPlatformSettings,
  })

  return {
    settings: query.data ?? null,
    error: query.error ? toErrorMessage(query.error) : null,
    isLoading: query.isPending,
  }
}
