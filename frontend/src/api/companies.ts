import type {
  Company,
  CompanyData,
  CompanyDetail,
  CreateCompanyRequest,
} from '../types/Company'
import { request } from './client'

export function createCompany(body: CreateCompanyRequest): Promise<Company> {
  return request<Company>('/api/superadmin/companies', {
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

export function resendActivation(id: string): Promise<void> {
  return request<void>(`/api/superadmin/companies/${id}/resend-activation`, {
    method: 'POST',
  })
}
