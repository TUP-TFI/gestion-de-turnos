import type { Company, CompanyData, CreateCompanyRequest } from '../types/Company'
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

export function getCompany(id: string): Promise<Company> {
  return request<Company>(`/api/superadmin/companies/${id}`)
}

export function updateCompany(id: string, body: CompanyData): Promise<Company> {
  return request<Company>(`/api/superadmin/companies/${id}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}
