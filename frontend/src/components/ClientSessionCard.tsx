import { Link } from 'react-router'
import { AuthCard } from './AuthCard'
import {
  PRIMARY_BUTTON_CLASSES,
  SECONDARY_BUTTON_CLASSES,
} from '../lib/formStyles'
import type { ClientSession } from '../types/Auth'

interface ClientSessionCardProps {
  session: ClientSession
  onLogout: () => void
}

// Lo que ven el login y el registro del cliente cuando ya tiene la sesión iniciada en esa empresa.
export function ClientSessionCard({ session, onLogout }: ClientSessionCardProps) {
  function handleLogout() {
    if (window.confirm('¿Cerrar sesión?')) {
      onLogout()
    }
  }

  return (
    <AuthCard
      title="Ya iniciaste sesión"
      subtitle={`Como ${session.user.firstName} ${session.user.lastName}`}
    >
      <div className="mt-6 space-y-3">
        <Link
          to={`/${session.slug}`}
          className={`w-full ${PRIMARY_BUTTON_CLASSES}`}
        >
          Ir a la página de la empresa
        </Link>
        <button
          type="button"
          onClick={handleLogout}
          className={`w-full ${SECONDARY_BUTTON_CLASSES}`}
        >
          Cerrar sesión
        </button>
      </div>
    </AuthCard>
  )
}
