import { PanelLayout } from '../../components/PanelLayout'
import type { PanelNavItem } from '../../components/PanelLayout'

const NAV_ITEMS: PanelNavItem[] = [
  { to: '/company/dashboard', label: 'Inicio' },
  { to: '/company/services', label: 'Servicios' },
]

export function CompanyLayout() {
  return <PanelLayout title="Panel de empresa" navItems={NAV_ITEMS} />
}
