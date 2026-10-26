import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { countOutOfRangeAppointments, saveBusinessHours } from '../api/businessHours'
import { toErrorMessage } from '../api/client'
import type { BusinessHoursSlot } from '../types/BusinessHours'

interface UseSaveBusinessHoursResult {
  save: (hours: BusinessHoursSlot[]) => void
  saved: boolean
  error: string | null
  isLoading: boolean
  reset: () => void
}

function outOfRangeMessage(count: number): string {
  const turnos = count === 1 ? 'Hay 1 turno' : `Hay ${count} turnos`
  return `${turnos} fuera del nuevo horario. No se cancelan: siguen en pie. ¿Guardar igual?`
}

export function useSaveBusinessHours(): UseSaveBusinessHoursResult {
  const queryClient = useQueryClient()
  // El conteo se pide fuera de la mutación, así que su estado y su error se guardan aparte.
  const [isCounting, setIsCounting] = useState(false)
  const [countError, setCountError] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: saveBusinessHours,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['business-hours'] }),
  })

  function reset() {
    setCountError(null)
    mutation.reset()
  }

  // Antes de guardar se cuentan los turnos que quedan fuera del horario nuevo (US-05.4). Si hay, el admin
  // confirma; si no, se guarda directo. El conteo valida lo mismo que el guardado, así que los errores de
  // franjas superpuestas o mal cargadas aparecen acá.
  async function save(hours: BusinessHoursSlot[]) {
    reset()

    let count: number
    setIsCounting(true)
    try {
      count = await countOutOfRangeAppointments(hours)
    } catch (error) {
      setCountError(toErrorMessage(error as Error))
      return
    } finally {
      setIsCounting(false)
    }

    if (count > 0 && !window.confirm(outOfRangeMessage(count))) {
      return
    }
    mutation.mutate(hours)
  }

  return {
    save,
    saved: mutation.isSuccess,
    error: countError ?? (mutation.error ? toErrorMessage(mutation.error) : null),
    isLoading: isCounting || mutation.isPending,
    reset,
  }
}
