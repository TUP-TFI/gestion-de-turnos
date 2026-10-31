import type { AppointmentStatus, ClientAppointment } from '../types/Appointment'
import { request } from './client'

// Trae los turnos del cliente logueado (US-07.6). Sin status, trae todos.
export function getClientAppointments(
  status?: AppointmentStatus,
): Promise<ClientAppointment[]> {
  const query = status ? `?status=${status}` : ''
  return request<ClientAppointment[]>(`/api/client/appointments${query}`, {
    auth: 'client',
  })
}
