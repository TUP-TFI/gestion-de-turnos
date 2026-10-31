import type {
  AppointmentStatus,
  ClientAppointment,
  CreateAppointmentRequest,
} from '../types/Appointment'
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

// Reserva un turno a nombre del cliente logueado (US-07.2).
export function createAppointment(
  body: CreateAppointmentRequest,
): Promise<ClientAppointment> {
  return request<ClientAppointment>('/api/client/appointments', {
    method: 'POST',
    body: JSON.stringify(body),
    auth: 'client',
  })
}
