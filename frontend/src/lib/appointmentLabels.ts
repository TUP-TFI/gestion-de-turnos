import { BADGE_VARIANT_CLASSES } from './formStyles'
import type {
  AppointmentOrigin,
  AppointmentStatus,
  CancelledBy,
} from '../types/Appointment'

export const STATUS_LABELS: Record<AppointmentStatus, string> = {
  PENDING: 'Pendiente',
  COMPLETED: 'Completado',
  CANCELLED: 'Cancelado',
}

export const STATUS_CLASSES: Record<AppointmentStatus, string> = {
  PENDING: BADGE_VARIANT_CLASSES.warning,
  COMPLETED: BADGE_VARIANT_CLASSES.success,
  CANCELLED: BADGE_VARIANT_CLASSES.neutral,
}

export const ORIGIN_LABELS: Record<AppointmentOrigin, string> = {
  ONLINE: 'Online',
  MANUAL: 'Manual',
}

export const CANCELLED_BY_LABELS: Record<CancelledBy, string> = {
  CLIENT: 'Cliente',
  COMPANY: 'Empresa',
}
