export interface ScheduleException {
  id: string
  companyId: string
  // Solo la fecha (ej. '2026-12-25').
  exceptionDate: string
  isClosed: boolean
  // Los tres siguientes son nulos si isClosed es true.
  startTime: string | null
  endTime: string | null
  intervalMinutes: number | null
  reason: string | null
  createdAt: string
  updatedAt: string
}
