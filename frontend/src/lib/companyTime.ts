// Fechas y horas en la zona de la empresa, nunca en la del navegador.

// Fecha de hoy en esa zona, con el formato YYYY-MM-DD que usan el input de fecha y el backend.
export function todayInZone(timezone: string): string {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: timezone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date())
  const part = (type: string) => parts.find((item) => item.type === type)?.value

  return `${part('year')}-${part('month')}-${part('day')}`
}

// Solo la hora de un instante UTC, para los botones de horario: 09:00.
export function formatTimeInZone(instant: string, timezone: string): string {
  return new Intl.DateTimeFormat('es-AR', {
    timeZone: timezone,
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).format(new Date(instant))
}
