import type { PublicPage } from '../types/PublicCompany'
import { request } from './client'

// Trae los datos públicos de la empresa y sus servicios activos (US-06.1). No necesita sesión.
export function getPublicCompany(slug: string): Promise<PublicPage> {
  return request<PublicPage>(
    `/api/public/companies/${encodeURIComponent(slug)}`,
    { auth: false },
  )
}
