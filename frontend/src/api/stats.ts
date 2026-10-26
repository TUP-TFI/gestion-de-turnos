import type { SuperadminStats } from '../types/SuperadminStats'
import { request } from './client'

export function getSuperadminStats(): Promise<SuperadminStats> {
  return request<SuperadminStats>('/api/superadmin/stats')
}
