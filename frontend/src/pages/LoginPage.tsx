import { useState } from 'react'
import { Navigate } from 'react-router'
import { useAuth } from '../hooks/useAuth'
import { useLogin } from '../hooks/useLogin'
import { getDashboardPath } from '../lib/panelRoutes'

const INPUT_CLASSES =
  'mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-slate-900 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand-light/50'

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
    <main className="flex min-h-screen items-center justify-center bg-slate-50 px-4 py-8">
      <div className="w-full max-w-sm rounded-xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <h1 className="text-center text-2xl font-semibold text-slate-900">
          Gestión de turnos
        </h1>
        <p className="mt-1 text-center text-sm text-slate-600">
          Ingresá al panel
        </p>

        <form
          className="mt-6 space-y-4"
          onSubmit={(event) => {
            event.preventDefault()
            login({ email, password })
          }}
        >
          <div>
            <label
              htmlFor="email"
              className="block text-sm font-medium text-slate-700"
            >
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
            <label
              htmlFor="password"
              className="block text-sm font-medium text-slate-700"
            >
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
            <p
              role="alert"
              className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700"
            >
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={isLoading}
            className="w-full cursor-pointer rounded-lg bg-brand px-4 py-2 font-medium text-white hover:bg-brand-dark focus:outline-none focus:ring-2 focus:ring-brand-light disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isLoading ? 'Ingresando...' : 'Ingresar'}
          </button>
        </form>
      </div>
    </main>
  )
}
