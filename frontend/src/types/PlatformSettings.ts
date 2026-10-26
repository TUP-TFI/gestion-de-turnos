// Cuerpo de PUT /api/superadmin/settings: los cuatro valores, sin updatedAt.
export type PlatformSettingsData = Omit<PlatformSettings, 'updatedAt'>

export interface PlatformSettings {
  minBookingNoticeMinutes: number
  cancellationDeadlineHours: number
  maxRescheduleCount: number
  maxBookingAdvanceDays: number
  updatedAt: string
}
