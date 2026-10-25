import { PanelLayout } from '../../components/PanelLayout'
import type { PanelNavItem } from '../../components/PanelLayout'

const NAV_ITEMS: PanelNavItem[] = [
  { to: '/superadmin/dashboard', label: 'Inicio' },
  { to: '/superadmin/companies', label: 'Empresas' },
]

export function SuperadminLayout() {
  return <PanelLayout title="Panel de sistema" navItems={NAV_ITEMS} />
}
