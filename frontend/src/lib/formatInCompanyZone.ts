// Muestra un instante UTC en la hora de la empresa, no en la de quien mira la pantalla: 02/11/2026, 09:00.
export function formatInCompanyZone(instant: string, timezone: string): string {
  return new Intl.DateTimeFormat('es-AR', {
    timeZone: timezone,
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).format(new Date(instant))
}
