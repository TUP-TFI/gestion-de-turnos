import type { CompanyClient } from '../types/Client'
import type { PageResponse } from '../types/Page'
import { request } from './client'

// Clientes de la empresa del admin logueado, por apellido y nombre (US-09.7). Página desde 0. Con texto, busca por
// nombre completo, email o teléfono, sin importar mayúsculas; el nombre también sin tildes. Sin texto, trae todos.
export function listCompanyClients(
  q: string,
  page: number,
): Promise<PageResponse<CompanyClient>> {
  const params = new URLSearchParams()
  const text = q.trim()
  if (text) {
    params.set('q', text)
  }
  params.set('page', String(page))

  return request<PageResponse<CompanyClient>>(
    `/api/company/clients?${params.toString()}`,
  )
}
