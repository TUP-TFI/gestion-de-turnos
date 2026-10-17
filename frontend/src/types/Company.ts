import type { User } from './User'

export type CompanyStatus = 'ACTIVE' | 'INACTIVE'

export type CompanyCategory =
  | 'BARBERSHOP'
  | 'HAIR_SALON'
  | 'BEAUTY_CENTER'
  | 'MEDICAL_OFFICE'
  | 'DENTAL_OFFICE'
  | 'VETERINARY'
  | 'NUTRITION'
  | 'PSYCHOLOGY'
  | 'PHYSIOTHERAPY'
  | 'TATTOO_STUDIO'
  | 'SPA'
  | 'OTHER'

export interface Company {
  id: string
  name: string
  // Parte de la URL pública (/{slug}). No editable.
  slug: string
  description: string | null
  address: string | null
  phone: string | null
  contactEmail: string | null
  category: CompanyCategory
  // Zona horaria de la empresa (ej. 'America/Argentina/Cordoba').
  timezone: string
  logoUrl: string | null
  // Formato #RRGGBB.
  primaryColor: string | null
  status: CompanyStatus
  createdAt: string
  updatedAt: string
}

// Datos editables de la empresa: el cuerpo de PUT /api/superadmin/companies/{id}. El slug y el status no se editan.
export type CompanyData = Pick<
  Company,
  | 'name'
  | 'category'
  | 'timezone'
  | 'description'
  | 'address'
  | 'phone'
  | 'contactEmail'
  | 'logoUrl'
>

// Cuerpo de POST /api/superadmin/companies. El slug y el status los genera el backend.
export interface CreateCompanyRequest {
  company: CompanyData
  admin: Pick<User, 'firstName' | 'lastName' | 'email' | 'phone'>
}
