import type { PlatformSettingsData } from '../types/PlatformSettings'

// Arma los parámetros desde el form de SettingsPage. Los inputs son type="number": el navegador ya
// valida que sean enteros dentro del rango, el back lo vuelve a validar.
export function readSettingsData(data: FormData): PlatformSettingsData {
  return {
    minBookingNoticeMinutes: Number(data.get('minBookingNoticeMinutes')),
    cancellationDeadlineHours: Number(data.get('cancellationDeadlineHours')),
    maxRescheduleCount: Number(data.get('maxRescheduleCount')),
    maxBookingAdvanceDays: Number(data.get('maxBookingAdvanceDays')),
  }
}
