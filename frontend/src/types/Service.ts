export type ServiceStatus = 'ACTIVE' | 'INACTIVE'

export interface Service {
  id: string
  companyId: string
  name: string
  description: string | null
  price: number
  durationMinutes: number
  status: ServiceStatus
  createdAt: string
  updatedAt: string
}
