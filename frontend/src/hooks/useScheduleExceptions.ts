import { useQuery } from '@tanstack/react-query'
import { toErrorMessage } from '../api/client'
import { listScheduleExceptions } from '../api/scheduleExceptions'
import type { ScheduleException } from '../types/ScheduleException'

interface UseScheduleExceptionsResult {
  exceptions: ScheduleException[]
  error: string | null
  isLoading: boolean
}

export function useScheduleExceptions(): UseScheduleExceptionsResult {
  const query = useQuery({
    queryKey: ['schedule-exceptions'],
    queryFn: listScheduleExceptions,
  })

  return {
    exceptions: query.data ?? [],
    error: query.error ? toErrorMessage(query.error) : null,
    isLoading: query.isPending,
  }
}
