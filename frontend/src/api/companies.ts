import type { Company, CreateCompanyRequest } from '../types/Company'
import { request } from './client'

export function createCompany(body: CreateCompanyRequest): Promise<Company> {
  return request<Company>('/api/superadmin/companies', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}
