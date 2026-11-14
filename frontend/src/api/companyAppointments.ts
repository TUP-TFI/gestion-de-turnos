import type { Availability } from '../types/PublicCompany'
import type {
  AdminAppointment,
  ManualAppointmentRequest,
  AppointmentOrigin,
  AppointmentStatus,
  AppointmentSummary,
} from '../types/Appointment'
import type { PageResponse } from '../types/Page'
import { request } from './client'

// Filtros de la agenda. Los que no se usan quedan sin valor y no viajan en la consulta.
export interface AppointmentFilters {
  status?: AppointmentStatus
  origin?: AppointmentOrigin
  rescheduled?: boolean
}

// Turnos de hoy y próximos 5 de la empresa del admin logueado (US-09.1).
export function getAppointmentSummary(): Promise<AppointmentSummary> {
  return request<AppointmentSummary>('/api/company/appointments/summary')
}

// Agenda de la empresa del admin logueado, paginada y del más lejano al más cercano (US-09.2). Página desde 0.
export function listCompanyAppointments(
  filters: AppointmentFilters,
  page: number,
): Promise<PageResponse<AdminAppointment>> {
  const params = new URLSearchParams()
  if (filters.status) {
    params.set('status', filters.status)
  }
  if (filters.origin) {
    params.set('origin', filters.origin)
  }
  if (filters.rescheduled !== undefined) {
    params.set('rescheduled', String(filters.rescheduled))
  }
  params.set('page', String(page))

  return request<PageResponse<AdminAppointment>>(
    `/api/company/appointments?${params.toString()}`,
  )
}

// Cancela un turno de la empresa del admin logueado (US-09.3). Sin plazo. El motivo es opcional: vacío o en blanco
// va como `{}`, y el backend lo guarda sin motivo.
export function cancelCompanyAppointment(
  id: string,
  reason: string,
): Promise<AdminAppointment> {
  const trimmed = reason.trim()
  return request<AdminAppointment>(`/api/company/appointments/${id}/cancel`, {
    method: 'POST',
    body: JSON.stringify(trimmed ? { reason: trimmed } : {}),
  })
}

// Carga un turno manual en la empresa del admin logueado (US-09.5 y 09.6). Pasa por las mismas validaciones que la
// reserva online y responde 201 con el turno en PENDING y origin MANUAL.
export function createManualAppointment(
  body: ManualAppointmentRequest,
): Promise<AdminAppointment> {
  return request<AdminAppointment>('/api/company/appointments', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

// Horarios libres de un servicio de la empresa del admin logueado en una fecha local de la empresa, con formato
// YYYY-MM-DD. Los mismos que ve la página pública, pero con la empresa de la sesión.
export function getCompanyAvailability(
  serviceId: string,
  date: string,
): Promise<Availability> {
  return request<Availability>(
    `/api/company/services/${serviceId}/availability?date=${date}`,
  )
}
