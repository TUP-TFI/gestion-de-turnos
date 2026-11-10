export type AppointmentOrigin = 'ONLINE' | 'MANUAL'

export type AppointmentStatus = 'PENDING' | 'COMPLETED' | 'CANCELLED'

export type CancelledBy = 'CLIENT' | 'COMPANY'

// Por qué el cliente no puede reprogramar un turno (RescheduleBlockedReason del backend).
export type RescheduleBlockedReason =
  | 'NOT_PENDING'
  | 'DEADLINE_PASSED'
  | 'LIMIT_REACHED'

export interface Appointment {
  id: string
  companyId: string
  serviceId: string
  // Nulo en reservas manuales sin cuenta (ver manualClientName/manualClientPhone).
  clientId: string | null
  manualClientName: string | null
  manualClientPhone: string | null
  priceSnapshot: number
  durationMinutesSnapshot: number
  startDateTime: string
  endDateTime: string
  previousStartDateTime: string | null
  rescheduleCount: number
  origin: AppointmentOrigin
  status: AppointmentStatus
  cancelledBy: CancelledBy | null
  cancellationReason: string | null
  cancelledAt: string | null
  createdAt: string
  updatedAt: string
}

// Un turno de "Mis turnos" (GET /api/client/appointments).
export type ClientAppointment = Pick<
  Appointment,
  | 'id'
  | 'serviceId'
  | 'startDateTime'
  | 'endDateTime'
  | 'status'
  | 'rescheduleCount'
  | 'priceSnapshot'
  | 'durationMinutesSnapshot'
> & {
  serviceName: string
  // Los calcula el backend con PlatformSettings: el front no conoce el plazo ni el tope (D1 de contratos-api.md).
  canCancel: boolean
  canReschedule: boolean
  rescheduleBlockedReason: RescheduleBlockedReason | null
}

// Cuerpo de POST /api/client/appointments. La empresa y el cliente salen de la sesión.
export type CreateAppointmentRequest = Pick<
  Appointment,
  'serviceId' | 'startDateTime'
>

// Un turno de la agenda del admin (GET /api/company/appointments y /summary). El cliente es el de la cuenta; en un
// turno manual sin cuenta, id es null y el nombre y el teléfono salen del turno.
export type AdminAppointment = Pick<
  Appointment,
  | 'id'
  | 'serviceId'
  | 'startDateTime'
  | 'endDateTime'
  | 'status'
  | 'origin'
  | 'rescheduleCount'
  | 'previousStartDateTime'
  | 'priceSnapshot'
  | 'durationMinutesSnapshot'
  | 'cancelledBy'
  | 'cancellationReason'
  | 'cancelledAt'
> & {
  serviceName: string
  client: { id: string | null; name: string; phone: string | null }
}

// Respuesta de GET /api/company/appointments/summary, sin turnos cancelados. Los dos van del más cercano al más lejano.
export interface AppointmentSummary {
  // Los de hoy, en la fecha local de la empresa.
  today: AdminAppointment[]
  // Los próximos 5 después de hoy.
  upcoming: AdminAppointment[]
}
