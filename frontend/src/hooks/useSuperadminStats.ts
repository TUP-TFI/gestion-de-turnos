import { useQuery } from '@tanstack/react-query'
import { toErrorMessage } from '../api/client'
import { getSuperadminStats } from '../api/stats'
import type { SuperadminStats } from '../types/SuperadminStats'

interface UseSuperadminStatsResult {
  stats: SuperadminStats | null
  error: string | null
  isLoading: boolean
}

export function useSuperadminStats(): UseSuperadminStatsResult {
  const query = useQuery({
    queryKey: ['stats'],
    queryFn: getSuperadminStats,
  })

  return {
    stats: query.data ?? null,
    error: query.error ? toErrorMessage(query.error) : null,
    isLoading: query.isPending,
  }
}
