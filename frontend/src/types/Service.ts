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

// Cuerpo de POST y PUT /api/company/services. La empresa sale de la sesión y el status se cambia con
// los endpoints de desactivar y reactivar.
export type ServiceData = Pick<
  Service,
  'name' | 'description' | 'price' | 'durationMinutes'
>
