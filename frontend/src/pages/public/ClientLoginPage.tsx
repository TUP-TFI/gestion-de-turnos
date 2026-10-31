import { Link, Navigate, useLocation, useParams } from 'react-router'
import { AuthCard } from '../../components/AuthCard'
import { CompanyNotFoundCard } from '../../components/CompanyNotFoundCard'
import { useClientAuth } from '../../hooks/useClientAuth'
import { useClientLogin } from '../../hooks/useClientLogin'
import { usePublicCompany } from '../../hooks/usePublicCompany'
import {
  ERROR_CLASSES,
  INPUT_CLASSES,
  LABEL_CLASSES,
  LINK_CLASSES,
  MUTED_TEXT_CLASSES,
  PRIMARY_BUTTON_CLASSES,
} from '../../lib/formStyles'

// Login del cliente en la página pública de una empresa (US-03.6). La cuenta sirve solo en esa empresa.
export function ClientLoginPage() {
  const { slug = '' } = useParams()
  const { search } = useLocation()
  const { session } = useClientAuth(slug)
  const { login, error, isLoading } = useClientLogin(slug)
  const { company, notFound } = usePublicCompany(slug)

  // Con sesión va a la página de la empresa. Si venía de elegir un horario, ahí se abre la confirmación (US-07.1).
  if (session) {
    return <Navigate to={`/${slug}${search}`} replace />
  }

  if (notFound) {
    return <CompanyNotFoundCard />
  }

  return (
    <AuthCard
      title="Iniciá sesión"
      subtitle={
        company
          ? `Para reservar tus turnos en ${company.name}`
          : 'Para reservar tus turnos'
      }
    >
      <form
        className="mt-6 space-y-4"
        onSubmit={(event) => {
          event.preventDefault()
          // Sin estado: al cerrar sesión el formulario vuelve vacío, sin la contraseña anterior.
          const form = new FormData(event.currentTarget)
          login({
            email: String(form.get('email')),
            password: String(form.get('password')),
          })
        }}
      >
        <div>
          <label htmlFor="email" className={LABEL_CLASSES}>
            Email
          </label>
          <input
            id="email"
            name="email"
            type="email"
            autoComplete="email"
            required
            className={INPUT_CLASSES}
          />
        </div>

        <div>
          <label htmlFor="password" className={LABEL_CLASSES}>
            Contraseña
          </label>
          <input
            id="password"
            name="password"
            type="password"
            autoComplete="current-password"
            required
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

      <p className={`mt-6 text-center ${MUTED_TEXT_CLASSES}`}>
        ¿No tenés cuenta?{' '}
        <Link to={`/${slug}/register${search}`} className={LINK_CLASSES}>
          Registrate
        </Link>
      </p>
    </AuthCard>
  )
}
