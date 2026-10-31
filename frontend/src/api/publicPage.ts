import type { PublicPage } from '../types/PublicPage'
import { request } from './client'

// Empresa y servicios activos de la página pública (US-06.1). No pide sesión.
export function getPublicPage(slug: string): Promise<PublicPage> {
  return request<PublicPage>(
    `/api/public/companies/${encodeURIComponent(slug)}`,
    { auth: false },
  )
}
