import { useState } from 'react'
import { Navigate } from 'react-router'
import { AuthCard } from '../components/AuthCard'
import { useAuth } from '../hooks/useAuth'
import { useLogin } from '../hooks/useLogin'
import {
  ERROR_CLASSES,
  INPUT_CLASSES,
  LABEL_CLASSES,
  PRIMARY_BUTTON_CLASSES,
} from '../lib/formStyles'
import { getDashboardPath } from '../lib/panelRoutes'

export function LoginPage() {
  const { session } = useAuth()
  const { login, error, isLoading } = useLogin()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const dashboardPath = session ? getDashboardPath(session.user.role) : null
  if (dashboardPath) {
    return <Navigate to={dashboardPath} replace />
  }

  return (
    <AuthCard title="Gestión de turnos" subtitle="Ingresá al panel">
      <form
        className="mt-6 space-y-4"
        onSubmit={(event) => {
          event.preventDefault()
          login({ email, password })
        }}
      >
        <div>
          <label htmlFor="email" className={LABEL_CLASSES}>
            Email
          </label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            className={INPUT_CLASSES}
          />
        </div>

        <div>
          <label htmlFor="password" className={LABEL_CLASSES}>
            Contraseña
          </label>
          <input
            id="password"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            className={INPUT_CLASSES}
          />
        </div>

        {error && (
          <p role="alert" className={ERROR_CLASSES}>
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={isLoading}
          className={`w-full ${PRIMARY_BUTTON_CLASSES}`}
        >
          {isLoading ? 'Ingresando...' : 'Ingresar'}
        </button>
      </form>
    </AuthCard>
  )
}
