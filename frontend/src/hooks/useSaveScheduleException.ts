import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toErrorMessage } from '../api/client'
import {
  countAffectedAppointments,
  createScheduleException,
  updateScheduleException,
} from '../api/scheduleExceptions'
import { findScheduleExceptionError } from '../lib/scheduleExceptionForm'
import type { ScheduleExceptionData } from '../types/ScheduleException'

interface UseSaveScheduleExceptionResult {
  save: (data: ScheduleExceptionData) => void
  saved: boolean
  error: string | null
  isLoading: boolean
  reset: () => void
}

function affectedMessage(count: number): string {
  const turnos = count === 1 ? 'Hay 1 turno' : `Hay ${count} turnos`
  return `${turnos} de ese día fuera de la excepción. No se cancelan: siguen en pie. ¿Guardar igual?`
}

// Sin id crea una excepción nueva; con id edita esa.
export function useSaveScheduleException(id?: string): UseSaveScheduleExceptionResult {
  const queryClient = useQueryClient()
  // La validación y el conteo pasan fuera de la mutación, así que su estado y su error se guardan aparte.
  const [isCounting, setIsCounting] = useState(false)
  const [checkError, setCheckError] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: (data: ScheduleExceptionData) =>
      id ? updateScheduleException(id, data) : createScheduleException(data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['schedule-exceptions'] }),
  })

  function reset() {
    setCheckError(null)
    mutation.reset()
  }

  // Antes de guardar se cuentan los turnos de esa fecha que quedan fuera de la excepción (US-05.6). Si hay,
  // el admin confirma; si no, se guarda directo.
  async function save(data: ScheduleExceptionData) {
    reset()

    const invalid = findScheduleExceptionError(data)
    if (invalid) {
      setCheckError(invalid)
      return
    }

    let count: number
    setIsCounting(true)
    try {
      count = await countAffectedAppointments(data)
    } catch (error) {
      setCheckError(toErrorMessage(error as Error))
      return
    } finally {
      setIsCounting(false)
    }

    if (count > 0 && !window.confirm(affectedMessage(count))) {
      return
    }
    mutation.mutate(data)
  }

  return {
    save,
    saved: mutation.isSuccess,
    error: checkError ?? (mutation.error ? toErrorMessage(mutation.error) : null),
    isLoading: isCounting || mutation.isPending,
    reset,
  }
}
