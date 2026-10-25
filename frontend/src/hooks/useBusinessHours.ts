import { useQuery } from '@tanstack/react-query'
import { getBusinessHours } from '../api/businessHours'
import { toErrorMessage } from '../api/client'
import type { BusinessHours } from '../types/BusinessHours'

interface UseBusinessHoursResult {
  hours: BusinessHours[]
  error: string | null
  isLoading: boolean
}

export function useBusinessHours(): UseBusinessHoursResult {
  const query = useQuery({
    queryKey: ['business-hours'],
    queryFn: getBusinessHours,
  })

  return {
    hours: query.data ?? [],
    error: query.error ? toErrorMessage(query.error) : null,
    isLoading: query.isPending,
  }
}
