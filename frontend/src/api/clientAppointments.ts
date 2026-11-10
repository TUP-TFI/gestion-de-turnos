import type {
  AppointmentStatus,
  ClientAppointment,
  CreateAppointmentRequest,
} from '../types/Appointment'
import type { Availability } from '../types/PublicCompany'
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

// Cancela un turno del cliente logueado (US-08.1).
export function cancelAppointment(id: string): Promise<ClientAppointment> {
  return request<ClientAppointment>(`/api/client/appointments/${id}/cancel`, {
    method: 'POST',
    auth: 'client',
  })
}

// Horarios libres para reprogramar un turno en una fecha local de la empresa, con formato YYYY-MM-DD (US-08.2). No
// cuenta como ocupado al propio turno.
export function getRescheduleAvailability(
  id: string,
  date: string,
): Promise<Availability> {
  return request<Availability>(
    `/api/client/appointments/${id}/availability?date=${date}`,
    { auth: 'client' },
  )
}

// Pasa un turno del cliente logueado a otro horario (US-08.2). startDateTime es un instante UTC.
export function rescheduleAppointment(
  id: string,
  startDateTime: string,
): Promise<ClientAppointment> {
  return request<ClientAppointment>(
    `/api/client/appointments/${id}/reschedule`,
    {
      method: 'POST',
      body: JSON.stringify({ startDateTime }),
      auth: 'client',
    },
  )
}
