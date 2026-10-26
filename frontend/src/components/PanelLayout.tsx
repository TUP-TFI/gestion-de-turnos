import { NavLink, Outlet } from 'react-router'
import { useAuth } from '../hooks/useAuth'
import { SECONDARY_BUTTON_CLASSES } from '../lib/formStyles'

export interface PanelNavItem {
  to: string
  label: string
}

interface PanelLayoutProps {
  title: string
  navItems: PanelNavItem[]
}

// Layout común de los paneles (/superadmin/* y /company/*): header con título, navegación y cierre de sesión.
export function PanelLayout({ title, navItems }: PanelLayoutProps) {
  const { logout } = useAuth()

  function handleLogout() {
    if (window.confirm('¿Cerrar sesión?')) {
      logout()
    }
  }

  return (
    <div className="min-h-screen bg-surface">
      <header className="border-b border-hairline bg-white">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-3 px-4 py-3">
          <div className="flex flex-wrap items-center gap-6">
            <h1 className="text-lg font-semibold text-brand">{title}</h1>
            <nav className="flex gap-4 text-sm font-medium">
              {navItems.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  // Sin `end`: "Servicios" y "Empresas" quedan activos también en su alta y su edición.
                  className={({ isActive }) =>
                    isActive
                      ? 'text-brand-dark underline underline-offset-4'
                      : 'text-muted hover:text-brand'
                  }
                >
                  {item.label}
                </NavLink>
              ))}
            </nav>
          </div>
          <button
            type="button"
            onClick={handleLogout}
            className={SECONDARY_BUTTON_CLASSES}
          >
            Cerrar sesión
          </button>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}
