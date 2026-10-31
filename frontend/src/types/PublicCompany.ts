import type { Company } from './Company'
import type { Service } from './Service'

export type PublicCompany = Omit<Company, 'id' | 'createdAt' | 'updatedAt'>

export type PublicService = Pick<
  Service,
  'id' | 'name' | 'description' | 'price' | 'durationMinutes'
>

// Respuesta de GET /api/public/companies/{slug}. Si la empresa está inactiva, services viene vacío.
export interface PublicPage {
  company: PublicCompany
  services: PublicService[]
}

// Horarios libres de una fecha local de la empresa (YYYY-MM-DD). Los inicios llegan en UTC y ordenados.
export interface Availability {
  date: string
  starts: string[]
}
