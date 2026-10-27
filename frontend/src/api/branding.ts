import type { CompanyBranding, CompanyBrandingData } from '../types/CompanyBranding'
import { request } from './client'

// Trae el logo, el color y el slug de la empresa del admin logueado (US-05.7).
export function getBranding(): Promise<CompanyBranding> {
  return request<CompanyBranding>('/api/company/branding')
}

// Guarda el logo y el color. Siempre se mandan los dos: si uno va en null, el backend lo borra.
export function updateBranding(body: CompanyBrandingData): Promise<CompanyBranding> {
  return request<CompanyBranding>('/api/company/branding', {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}
