export type AppointmentOrigin = 'ONLINE' | 'MANUAL'

export type AppointmentStatus = 'PENDING' | 'COMPLETED' | 'CANCELLED'

export type CancelledBy = 'CLIENT' | 'COMPANY'

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
> & { serviceName: string }
