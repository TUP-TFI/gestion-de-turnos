import type { PlatformSettings, PlatformSettingsData } from '../types/PlatformSettings'
import { request } from './client'

export function getPlatformSettings(): Promise<PlatformSettings> {
  return request<PlatformSettings>('/api/superadmin/settings')
}

export function updatePlatformSettings(body: PlatformSettingsData): Promise<PlatformSettings> {
  return request<PlatformSettings>('/api/superadmin/settings', {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}
