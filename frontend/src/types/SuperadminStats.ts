// Indicadores del dashboard del superadmin: GET /api/superadmin/stats.
export interface SuperadminStats {
  activeCompanies: number
  inactiveCompanies: number
  totalAppointments: number
}
