import type { Service, ServiceData } from '../types/Service'
import { request } from './client'

export function listServices(): Promise<Service[]> {
  return request<Service[]>('/api/company/services')
}

export function getService(id: string): Promise<Service> {
  return request<Service>(`/api/company/services/${id}`)
}

export function createService(body: ServiceData): Promise<Service> {
  return request<Service>('/api/company/services', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function updateService(id: string, body: ServiceData): Promise<Service> {
  return request<Service>(`/api/company/services/${id}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}

export function deactivateService(id: string): Promise<Service> {
  return request<Service>(`/api/company/services/${id}/deactivate`, {
    method: 'POST',
  })
}

export function activateService(id: string): Promise<Service> {
  return request<Service>(`/api/company/services/${id}/activate`, {
    method: 'POST',
  })
}

// Turnos pendientes futuros del servicio, para avisar antes de desactivarlo (US-05.2).
export async function countFutureAppointments(id: string): Promise<number> {
  const { count } = await request<{ count: number }>(
    `/api/company/services/${id}/future-appointments`,
  )
  return count
}
