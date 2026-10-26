import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toErrorMessage } from '../api/client'
import {
  activateService,
  countFutureAppointments,
  deactivateService,
} from '../api/services'
import type { Service } from '../types/Service'

interface UseServiceStatusResult {
  toggleStatus: (service: Service) => void
  error: string | null
  isLoading: boolean
}

function deactivationMessage(name: string, count: number): string {
  if (count === 0) {
    return `¿Desactivar "${name}"? Va a dejar de ofrecerse para nuevas reservas.`
  }
  const turnos = count === 1 ? '1 turno futuro' : `${count} turnos futuros`
  return `"${name}" tiene ${turnos}. No se cancelan: siguen en pie. ¿Desactivar igual?`
}

function changeStatus(service: Service): Promise<Service> {
  return service.status === 'INACTIVE'
    ? activateService(service.id)
    : deactivateService(service.id)
}

export function useServiceStatus(): UseServiceStatusResult {
  const queryClient = useQueryClient()
  // El conteo se pide fuera de la mutación, así que su error se guarda aparte.
  const [countError, setCountError] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: changeStatus,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['services'] }),
  })

  // Reactiva un servicio inactivo. Uno activo se desactiva recién después de mostrar el conteo de turnos
  // futuros y de que el admin confirme (US-05.2): mientras el aviso está abierto no hay mutación en curso.
  async function toggleStatus(service: Service) {
    setCountError(null)
    mutation.reset()

    if (service.status === 'INACTIVE') {
      mutation.mutate(service)
      return
    }

    let count: number
    try {
      count = await countFutureAppointments(service.id)
    } catch (error) {
      setCountError(toErrorMessage(error as Error))
      return
    }
    if (window.confirm(deactivationMessage(service.name, count))) {
      mutation.mutate(service)
    }
  }

  return {
    toggleStatus,
    error: countError ?? (mutation.error ? toErrorMessage(mutation.error) : null),
    isLoading: mutation.isPending,
  }
}
