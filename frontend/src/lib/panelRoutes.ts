import type { UserRole } from '../types/User'

// Pantalla inicial del panel para cada rol. El cliente no tiene panel.
const DASHBOARD_PATHS: Record<UserRole, string | null> = {
  SUPERADMIN: '/superadmin/dashboard',
  COMPANY_ADMIN: '/company/dashboard',
  CLIENT: null,
}

export function getDashboardPath(role: UserRole): string | null {
  return DASHBOARD_PATHS[role]
}
