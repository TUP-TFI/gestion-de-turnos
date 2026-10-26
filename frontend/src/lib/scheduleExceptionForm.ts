import type { ScheduleExceptionData } from '../types/ScheduleException'
import { toNullable, toRequired } from './companyForm'

// Fecha de hoy ('AAAA-MM-DD') según el reloj de la computadora. No se usa toISOString() porque da la fecha
// en UTC: desde las 21:00 de Argentina ya devuelve el día siguiente.
export function todayDate(): string {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}

// '2026-12-25' → '25/12/2026'. Se arma con el texto y no con new Date(), que lo leería como medianoche UTC y
// en Argentina mostraría el día anterior.
export function formatDate(date: string): string {
  const [year, month, day] = date.split('-')
  return `${day}/${month}/${year}`
}

// Arma el body para la API con lo que cargó el admin en ScheduleExceptionFields. Si el día está cerrado, el
// form no muestra los campos de horario: no vienen en data y quedan en null.
export function readScheduleExceptionData(data: FormData): ScheduleExceptionData {
  const intervalMinutes = toNullable(data.get('intervalMinutes'))
  return {
    exceptionDate: toRequired(data.get('exceptionDate')),
    isClosed: data.get('isClosed') === 'on',
    startTime: toNullable(data.get('startTime')),
    endTime: toNullable(data.get('endTime')),
    intervalMinutes: intervalMinutes === null ? null : Number(intervalMinutes),
    reason: toNullable(data.get('reason')),
  }
}

// Primer error del form, o null si está bien. Revisa lo que el navegador no puede revisar solo: la fecha
// pasada y el fin antes del inicio. Si falta un campo obligatorio, ya lo frena el required del input.
export function findScheduleExceptionError(data: ScheduleExceptionData): string | null {
  if (data.exceptionDate < todayDate()) {
    return 'La fecha no puede ser anterior a hoy.'
  }
  if (data.startTime && data.endTime && data.endTime <= data.startTime) {
    return 'La hora de fin tiene que ser posterior a la de inicio.'
  }
  return null
}
