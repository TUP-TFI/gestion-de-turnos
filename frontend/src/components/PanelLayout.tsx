import { NavLink, Outlet } from 'react-router'
import { useAuth } from '../hooks/useAuth'

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

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-3xl flex-wrap items-center justify-between gap-3 px-4 py-3">
          <div className="flex flex-wrap items-center gap-6">
            <h1 className="text-lg font-semibold text-brand">{title}</h1>
            <nav className="flex gap-4 text-sm font-medium">
              {navItems.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) =>
                    isActive
                      ? 'text-brand-dark underline underline-offset-4'
                      : 'text-slate-600 hover:text-brand'
                  }
                >
                  {item.label}
                </NavLink>
              ))}
            </nav>
          </div>
          <button
            type="button"
            onClick={logout}
            className="cursor-pointer rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 focus:outline-none focus:ring-2 focus:ring-brand-light"
          >
            Cerrar sesión
          </button>
        </div>
      </header>
      <main className="mx-auto max-w-3xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}
