import type { BusinessHours, BusinessHoursSlot } from '../types/BusinessHours'
import { request } from './client'

export function getBusinessHours(): Promise<BusinessHours[]> {
  return request<BusinessHours[]>('/api/company/business-hours')
}

// Reemplaza la semana entera: un día sin franjas queda cerrado (US-05.3).
export function saveBusinessHours(hours: BusinessHoursSlot[]): Promise<BusinessHours[]> {
  return request<BusinessHours[]>('/api/company/business-hours', {
    method: 'PUT',
    body: JSON.stringify({ hours }),
  })
}

// Turnos pendientes futuros que quedarían fuera del horario propuesto, sin guardarlo (US-05.4).
export async function countOutOfRangeAppointments(
  hours: BusinessHoursSlot[],
): Promise<number> {
  const { count } = await request<{ count: number }>(
    '/api/company/business-hours/out-of-range-appointments',
    {
      method: 'POST',
      body: JSON.stringify({ hours }),
    },
  )
  return count
}
