import type { Availability, PublicPage } from '../types/PublicCompany'
import { request } from './client'

// Horarios libres de un servicio en una fecha local de la empresa, con formato YYYY-MM-DD (US-06.2). No necesita sesión.
export function getAvailability(
  slug: string,
  serviceId: string,
  date: string,
): Promise<Availability> {
  return request<Availability>(
    `/api/public/companies/${encodeURIComponent(slug)}/services/${serviceId}/availability?date=${date}`,
    { auth: false },
  )
}

// Trae los datos públicos de la empresa y sus servicios activos (US-06.1). No necesita sesión.
export function getPublicCompany(slug: string): Promise<PublicPage> {
  return request<PublicPage>(
    `/api/public/companies/${encodeURIComponent(slug)}`,
    { auth: false },
  )
}
