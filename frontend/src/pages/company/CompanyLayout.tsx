import { PanelLayout } from '../../components/PanelLayout'
import type { PanelNavItem } from '../../components/PanelLayout'

const NAV_ITEMS: PanelNavItem[] = [
  { to: '/company/dashboard', label: 'Inicio' },
  { to: '/company/services', label: 'Servicios' },
  { to: '/company/business-hours', label: 'Horario' },
  { to: '/company/schedule-exceptions', label: 'Feriados' },
  { to: '/company/branding', label: 'Marca' },
]

export function CompanyLayout() {
  return <PanelLayout title="Panel de empresa" navItems={NAV_ITEMS} />
}
