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
