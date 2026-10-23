import { useAuth } from '../../hooks/useAuth'
import {
  MUTED_TEXT_CLASSES,
  PAGE_TITLE_CLASSES,
  SECONDARY_BUTTON_CLASSES,
} from '../../lib/formStyles'

export function CompanyDashboardPage() {
  const { session, logout } = useAuth()

  return (
    <main className="min-h-screen bg-surface px-4 py-8">
      <div className="mx-auto max-w-5xl">
        <h1 className={PAGE_TITLE_CLASSES}>
          Panel de empresa
        </h1>
        <p className={`mt-1 ${MUTED_TEXT_CLASSES}`}>
          Hola, {session?.user.firstName}
        </p>
        <button
          type="button"
          onClick={logout}
          className={`mt-6 ${SECONDARY_BUTTON_CLASSES}`}
        >
          Cerrar sesión
        </button>
      </div>
    </main>
  )
}
