import type { ScheduleException, ScheduleExceptionData } from '../types/ScheduleException'
import { request } from './client'

// Excepciones de hoy en adelante, ordenadas por fecha (US-05.6).
export function listScheduleExceptions(): Promise<ScheduleException[]> {
  return request<ScheduleException[]>('/api/company/schedule-exceptions')
}

export function createScheduleException(body: ScheduleExceptionData): Promise<ScheduleException> {
  return request<ScheduleException>('/api/company/schedule-exceptions', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function updateScheduleException(
  id: string,
  body: ScheduleExceptionData,
): Promise<ScheduleException> {
  return request<ScheduleException>(`/api/company/schedule-exceptions/${id}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}

export function deleteScheduleException(id: string): Promise<void> {
  return request<void>(`/api/company/schedule-exceptions/${id}`, {
    method: 'DELETE',
  })
}

// Turnos pendientes futuros de esa fecha que quedarían fuera de la excepción propuesta, sin guardarla (US-05.6).
export async function countAffectedAppointments(body: ScheduleExceptionData): Promise<number> {
  const { count } = await request<{ count: number }>(
    '/api/company/schedule-exceptions/affected-appointments',
    {
      method: 'POST',
      body: JSON.stringify(body),
    },
  )
  return count
}
