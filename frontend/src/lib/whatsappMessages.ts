import type { AdminAppointment } from '../types/Appointment'
import { formatInCompanyZone } from './formatInCompanyZone'

// Los textos de los avisos por WhatsApp, uno por función. No van a la base ni al backend: se pueden ajustar acá.

interface ActivationMessage {
  // Solo en el alta. Al generar un link nuevo no se tiene.
  adminName?: string
  companyName: string
  activationLink: string
}

// Para el admin de una empresa nueva, o para el que pidió un link nuevo (US-04.1 y US-04.8).
export function activationMessage({
  adminName,
  companyName,
  activationLink,
}: ActivationMessage): string {
  const greeting = adminName ? `Hola ${adminName}` : 'Hola'
  return `${greeting}, te dimos de alta como administrador de ${companyName} en Gestión de Turnos. Activá tu cuenta desde este link: ${activationLink}. Vence en 48 horas.`
}

interface CompanyClosedMessage {
  clientName: string
  serviceName: string
  companyName: string
  // Instante UTC, tal como lo devolvió el backend.
  startDateTime: string
  timezone: string
}

// Para un cliente con un turno futuro cuando se desactiva la empresa (US-04.4).
export function companyClosedMessage({
  clientName,
  serviceName,
  companyName,
  startDateTime,
  timezone,
}: CompanyClosedMessage): string {
  const date = formatInCompanyZone(startDateTime, timezone)
  return `Hola ${clientName}, tu turno de ${serviceName} en ${companyName} del ${date} fue cancelado porque el negocio dejó de tomar turnos en la plataforma.`
}

interface AppointmentNoticeMessage {
  clientName: string
  serviceName: string
  companyName: string
  // Instante UTC, tal como lo devolvió el backend.
  startDateTime: string
  timezone: string
}

// Para un cliente cuyo turno el admin cargó o dejó pendiente (US-10.1).
export function confirmationMessage({
  clientName,
  serviceName,
  companyName,
  startDateTime,
  timezone,
}: AppointmentNoticeMessage): string {
  const date = formatInCompanyZone(startDateTime, timezone)
  return `Hola ${clientName}, tu turno de ${serviceName} en ${companyName} quedó confirmado para el ${date}.`
}

interface RescheduleMessage extends AppointmentNoticeMessage {
  // Fecha anterior, si el backend la tiene.
  previousStartDateTime: string | null
}

// Para un cliente cuyo turno se pasó a otra fecha (US-10.2).
export function rescheduleMessage({
  clientName,
  serviceName,
  companyName,
  startDateTime,
  previousStartDateTime,
  timezone,
}: RescheduleMessage): string {
  const date = formatInCompanyZone(startDateTime, timezone)
  const before = previousStartDateTime
    ? ` (antes: ${formatInCompanyZone(previousStartDateTime, timezone)})`
    : ''
  return `Hola ${clientName}, tu turno de ${serviceName} en ${companyName} se pasó al ${date}${before}.`
}

interface CancellationMessage extends AppointmentNoticeMessage {
  reason: string | null
}

// Para un cliente cuyo turno canceló el negocio (US-10.3).
export function cancellationMessage({
  clientName,
  serviceName,
  companyName,
  startDateTime,
  reason,
  timezone,
}: CancellationMessage): string {
  const date = formatInCompanyZone(startDateTime, timezone)
  const why = reason?.trim() ? ` Motivo: ${reason.trim()}.` : ''
  return `Hola ${clientName}, tu turno de ${serviceName} en ${companyName} del ${date} fue cancelado.${why}`
}

export type AppointmentNoticeKind = 'confirmation' | 'reschedule' | 'cancellation'

// Cómo se nombra cada aviso, por ejemplo en el nombre accesible del botón.
export const APPOINTMENT_NOTICE_LABELS: Record<AppointmentNoticeKind, string> = {
  confirmation: 'confirmación del turno',
  reschedule: 'cambio de horario',
  cancellation: 'cancelación',
}

// El aviso que corresponde al estado del turno (tipo y texto), o null si no hay aviso: los completados y los que
// canceló el cliente (ya lo sabe) van sin botón. Un pendiente reprogramado avisa la reprogramación; uno sin
// reprogramar, la confirmación.
export function appointmentNotice(
  appointment: AdminAppointment,
  companyName: string,
  timezone: string,
): { kind: AppointmentNoticeKind; text: string } | null {
  const base = {
    clientName: appointment.client.name,
    serviceName: appointment.serviceName,
    companyName,
    startDateTime: appointment.startDateTime,
    timezone,
  }

  if (appointment.status === 'PENDING') {
    return appointment.rescheduleCount > 0
      ? {
          kind: 'reschedule',
          text: rescheduleMessage({
            ...base,
            previousStartDateTime: appointment.previousStartDateTime,
          }),
        }
      : { kind: 'confirmation', text: confirmationMessage(base) }
  }
  if (appointment.status === 'CANCELLED' && appointment.cancelledBy === 'COMPANY') {
    return {
      kind: 'cancellation',
      text: cancellationMessage({
        ...base,
        reason: appointment.cancellationReason,
      }),
    }
  }
  return null
}

// Solo el texto de `appointmentNotice`.
export function appointmentMessage(
  appointment: AdminAppointment,
  companyName: string,
  timezone: string,
): string | null {
  return appointmentNotice(appointment, companyName, timezone)?.text ?? null
}
