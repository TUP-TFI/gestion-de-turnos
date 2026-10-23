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

// Reactiva un servicio inactivo. Uno activo se desactiva recién después de mostrar el conteo de turnos
// futuros y de que el admin confirme (US-05.2). Devuelve null si el admin cancela.
async function toggle(service: Service): Promise<Service | null> {
  if (service.status === 'INACTIVE') {
    return activateService(service.id)
  }
  const count = await countFutureAppointments(service.id)
  if (!window.confirm(deactivationMessage(service.name, count))) {
    return null
  }
  return deactivateService(service.id)
}

export function useServiceStatus(): UseServiceStatusResult {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: toggle,
    onSuccess: (result) => {
      if (result) {
        return queryClient.invalidateQueries({ queryKey: ['services'] })
      }
    },
  })

  return {
    toggleStatus: mutation.mutate,
    error: mutation.error ? toErrorMessage(mutation.error) : null,
    isLoading: mutation.isPending,
  }
}
