export type NotificationEventType =
  | 'BOOKING_CONFIRMED'
  | 'APPOINTMENT_CANCELLED'
  | 'APPOINTMENT_RESCHEDULED'
  | 'ACCOUNT_ACTIVATION'
  | 'PASSWORD_RESET'

export type NotificationChannel = 'WHATSAPP' | 'EMAIL'

export type NotificationStatus = 'PENDING' | 'SENT' | 'FAILED'

export interface Notification {
  id: string
  // Nulo en eventos de cuenta (ACCOUNT_ACTIVATION, PASSWORD_RESET).
  appointmentId: string | null
  // Nulo si el destinatario es un cliente manual sin cuenta.
  userId: string | null
  recipientPhone: string
  eventType: NotificationEventType
  channel: NotificationChannel
  status: NotificationStatus
  attemptCount: number
  // Nulo mientras status no sea SENT.
  sentAt: string | null
  errorMessage: string | null
  createdAt: string
}
