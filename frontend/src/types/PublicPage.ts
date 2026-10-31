import type { Company } from './Company'
import type { Service } from './Service'

// Datos de la empresa que ve un visitante. El back no manda el id ni las fechas.
export type PublicCompany = Pick<
  Company,
  | 'name'
  | 'slug'
  | 'description'
  | 'address'
  | 'phone'
  | 'contactEmail'
  | 'category'
  | 'timezone'
  | 'logoUrl'
  | 'primaryColor'
  | 'status'
>

// Solo los servicios activos, por eso no trae el status.
export type PublicService = Pick<
  Service,
  'id' | 'name' | 'description' | 'price' | 'durationMinutes'
>

// Respuesta de GET /api/public/companies/{slug}. Si la empresa está desactivada, services viene vacío.
export interface PublicPage {
  company: PublicCompany
  services: PublicService[]
}
