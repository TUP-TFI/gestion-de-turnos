export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'

export interface BusinessHours {
  id: string
  companyId: string
  dayOfWeek: DayOfWeek
  // Solo la hora (ej. '09:00:00'). La zona horaria está en Company.timezone.
  startTime: string
  endTime: string
  intervalMinutes: number
  createdAt: string
  updatedAt: string
}

// Una franja de PUT /api/company/business-hours y del conteo de turnos fuera de rango. Las horas van como
// 'HH:mm' y se interpretan en la zona horaria de la empresa.
export type BusinessHoursSlot = Pick<
  BusinessHours,
  'dayOfWeek' | 'startTime' | 'endTime' | 'intervalMinutes'
>
