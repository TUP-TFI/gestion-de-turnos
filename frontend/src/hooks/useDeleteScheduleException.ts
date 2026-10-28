import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toErrorMessage } from '../api/client'
import { deleteScheduleException } from '../api/scheduleExceptions'
import { formatDate } from '../lib/scheduleExceptionForm'
import type { ScheduleException } from '../types/ScheduleException'

interface UseDeleteScheduleExceptionResult {
  deleteException: (exception: ScheduleException) => void
  error: string | null
  isLoading: boolean
}

export function useDeleteScheduleException(): UseDeleteScheduleExceptionResult {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: (exception: ScheduleException) => deleteScheduleException(exception.id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['schedule-exceptions'] }),
  })

  // La confirmación va antes de la mutación: mientras el aviso está abierto no hay nada en curso.
  function deleteException(exception: ScheduleException) {
    mutation.reset()
    const message = `¿Borrar la excepción del ${formatDate(exception.exceptionDate)}? Ese día vuelve a tu horario semanal.`
    if (window.confirm(message)) {
      mutation.mutate(exception)
    }
  }

  return {
    deleteException,
    error: mutation.error ? toErrorMessage(mutation.error) : null,
    isLoading: mutation.isPending,
  }
}
