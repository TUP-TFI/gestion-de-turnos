import { useQuery } from '@tanstack/react-query'
import { toErrorMessage } from '../api/client'
import { getClientAppointments } from '../api/clientAppointments'
import type { AppointmentStatus, ClientAppointment } from '../types/Appointment'
import { useClientAuth } from './useClientAuth'

interface UseClientAppointmentsResult {
  appointments: ClientAppointment[]
  error: string | null
  isLoading: boolean
}

export function useClientAppointments(
  slug: string,
  status?: AppointmentStatus,
): UseClientAppointmentsResult {
  const { session } = useClientAuth(slug)
  const query = useQuery({
    // El slug separa los turnos de cada empresa: un cliente puede cambiar de empresa sin recargar la página.
    queryKey: ['client-appointments', slug, status],
    queryFn: () => getClientAppointments(status),
    enabled: session !== null,
  })

  return {
    appointments: query.data ?? [],
    error: query.error ? toErrorMessage(query.error) : null,
    isLoading: query.isPending,
  }
}
