import type { CancellationNotices } from '../types/CancellationNotice'
import type {
  ActivationLink,
  Company,
  CompanyData,
  CompanyDetail,
  CreatedCompany,
  CreateCompanyRequest,
} from '../types/Company'
import { request } from './client'

export function createCompany(
  body: CreateCompanyRequest,
): Promise<CreatedCompany> {
  return request<CreatedCompany>('/api/superadmin/companies', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function listCompanies(): Promise<Company[]> {
  return request<Company[]>('/api/superadmin/companies')
}

export function getCompany(id: string): Promise<CompanyDetail> {
  return request<CompanyDetail>(`/api/superadmin/companies/${id}`)
}

export function updateCompany(id: string, body: CompanyData): Promise<Company> {
  return request<Company>(`/api/superadmin/companies/${id}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}

export function deactivateCompany(id: string): Promise<Company> {
  return request<Company>(`/api/superadmin/companies/${id}/deactivate`, {
    method: 'POST',
  })
}

export function reactivateCompany(id: string): Promise<Company> {
  return request<Company>(`/api/superadmin/companies/${id}/reactivate`, {
    method: 'POST',
  })
}

// Genera un link de activación nuevo y lo devuelve: el anterior deja de servir. El sistema no lo envía.
export function resendActivation(id: string): Promise<ActivationLink> {
  return request<ActivationLink>(
    `/api/superadmin/companies/${id}/resend-activation`,
    { method: 'POST' },
  )
}

// Turnos futuros cancelados al desactivar la empresa, para avisar a cada cliente (US-04.4).
export function getCancellationNotices(
  id: string,
): Promise<CancellationNotices> {
  return request<CancellationNotices>(
    `/api/superadmin/companies/${id}/cancellation-notices`,
  )
}
