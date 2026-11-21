// Franja de un día abierto las 24 horas. La base exige que el fin sea posterior al inicio (chk_hours_range), así que
// el día completo se guarda de 00:00 a 23:59. El back toma una franja hasta las 23:59 como hasta la medianoche
// (DayWindow.END_OF_DAY): con turnos de 30 minutos, el último empieza a las 23:30 y termina a las 00:00.
export const FULL_DAY_START = '00:00'
export const FULL_DAY_END = '23:59'

export const FULL_DAY_LABEL = 'Abierto las 24 horas'

// Acepta 'HH:mm' (inputs) o 'HH:mm:ss' (backend).
export function isFullDay(startTime: string | null, endTime: string | null): boolean {
  return startTime?.slice(0, 5) === FULL_DAY_START && endTime?.slice(0, 5) === FULL_DAY_END
}
