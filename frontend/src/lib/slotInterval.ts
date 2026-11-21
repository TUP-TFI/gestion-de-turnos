import { FULL_DAY_END } from './fullDay'

// Valores más comunes de "Turnos cada". Cualquier otro se carga con "Otro".
export const INTERVAL_OPTIONS = [10, 15, 20, 30, 45, 60]

export function isIntervalOption(value: string): boolean {
  return INTERVAL_OPTIONS.some((option) => String(option) === value)
}

// Hasta cuántos horarios se nombran antes de resumir con "…".
const LISTED_STARTS = 3

// 'HH:mm' → minutos desde la medianoche.
function toMinutes(time: string): number {
  const [hours, minutes] = time.split(':').map(Number)
  return hours * 60 + minutes
}

function toTime(minutes: number): string {
  const hours = String(Math.floor(minutes / 60)).padStart(2, '0')
  return `${hours}:${String(minutes % 60).padStart(2, '0')}`
}

function joinWithY(items: string[]): string {
  return items.length === 1 ? items[0] : `${items.slice(0, -1).join(', ')} y ${items[items.length - 1]}`
}

/**
 * Vista previa de los horarios de inicio de una franja, para que el admin vea qué genera el intervalo. Toma un turno
 * que dura lo mismo que el intervalo: un servicio más largo usa varios seguidos. Una franja hasta las 23:59 cuenta hasta
 * la medianoche, igual que el back (DayWindow.END_OF_DAY). Con datos incompletos o inválidos devuelve null: esos errores
 * ya se muestran en la franja.
 */
export function describeStarts(startTime: string, endTime: string, intervalMinutes: string): string | null {
  const interval = Number(intervalMinutes)
  if (!startTime || !endTime || !Number.isInteger(interval) || interval < 1) {
    return null
  }
  const start = toMinutes(startTime)
  const end = endTime === FULL_DAY_END ? 24 * 60 : toMinutes(endTime)
  if (end <= start) {
    return null
  }

  const starts: string[] = []
  for (let time = start; time + interval <= end; time += interval) {
    starts.push(toTime(time))
  }
  if (starts.length === 0) {
    return `La franja dura menos de ${interval} min: no genera turnos.`
  }
  if (starts.length <= LISTED_STARTS + 1) {
    return `Los turnos empiezan a las ${joinWithY(starts)}.`
  }
  return `Los turnos empiezan a las ${starts.slice(0, LISTED_STARTS).join(', ')}… y el último a las ${starts[starts.length - 1]}.`
}
