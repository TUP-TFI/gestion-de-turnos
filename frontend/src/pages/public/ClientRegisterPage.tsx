import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { AuthCard } from '../../components/AuthCard'
import { ClientSessionCard } from '../../components/ClientSessionCard'
import { CompanyNotFoundCard } from '../../components/CompanyNotFoundCard'
import { useClientAuth } from '../../hooks/useClientAuth'
import { useClientRegister } from '../../hooks/useClientRegister'
import { usePublicCompany } from '../../hooks/usePublicCompany'
import {
  ERROR_CLASSES,
  INPUT_CLASSES,
  LABEL_CLASSES,
  LINK_CLASSES,
  MUTED_TEXT_CLASSES,
  PRIMARY_BUTTON_CLASSES,
} from '../../lib/formStyles'

// Los mismos límites que valida el backend.
const MIN_PASSWORD_LENGTH = 8
const MAX_PASSWORD_LENGTH = 72

// Registro del cliente en la página pública de una empresa (US-03.5). Al registrarse queda con la sesión iniciada.
export function ClientRegisterPage() {
  const { slug = '' } = useParams()
  const { session, logout } = useClientAuth(slug)
  const { register, error, isLoading } = useClientRegister(slug)
  const { company, notFound } = usePublicCompany(slug)
  const [passwordsMismatch, setPasswordsMismatch] = useState(false)

  if (session) {
    return <ClientSessionCard session={session} onLogout={logout} />
  }

  if (notFound) {
    return <CompanyNotFoundCard />
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const password = String(form.get('password'))
    const mismatch = password !== String(form.get('passwordConfirmation'))
    setPasswordsMismatch(mismatch)
    if (mismatch) {
      return
    }

    register({
      firstName: String(form.get('firstName')),
      lastName: String(form.get('lastName')),
      email: String(form.get('email')),
      phone: String(form.get('phone')),
      password,
    })
  }

  const message = passwordsMismatch ? 'Las contraseñas no coinciden' : error

  return (
    <AuthCard
      title="Creá tu cuenta"
      subtitle={
        company
          ? `Para reservar tus turnos en ${company.name}`
          : 'Para reservar tus turnos'
      }
      wide
    >
      {/* De a dos campos por fila desde tablet; en el celular, uno abajo del otro. */}
      <form
        className="mt-6 grid gap-4 sm:grid-cols-2"
        onSubmit={handleSubmit}
      >
        <div>
          <label htmlFor="firstName" className={LABEL_CLASSES}>
            Nombre
          </label>
          <input
            id="firstName"
            name="firstName"
            autoComplete="given-name"
            required
            maxLength={80}
            className={INPUT_CLASSES}
          />
        </div>

        <div>
          <label htmlFor="lastName" className={LABEL_CLASSES}>
            Apellido
          </label>
          <input
            id="lastName"
            name="lastName"
            autoComplete="family-name"
            required
            maxLength={80}
            className={INPUT_CLASSES}
          />
        </div>

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
            maxLength={150}
            className={INPUT_CLASSES}
          />
        </div>

        <div>
          <label htmlFor="phone" className={LABEL_CLASSES}>
            Teléfono
          </label>
          <input
            id="phone"
            name="phone"
            type="tel"
            autoComplete="tel"
            required
            maxLength={30}
            className={INPUT_CLASSES}
          />
          {/* El teléfono es obligatorio porque los avisos de los turnos llegan por WhatsApp. */}
          <p className={`mt-1 ${MUTED_TEXT_CLASSES}`}>
            Te avisamos por WhatsApp de tus turnos.
          </p>
        </div>

        <div>
          <label htmlFor="password" className={LABEL_CLASSES}>
            Contraseña
          </label>
          <input
            id="password"
            name="password"
            type="password"
            autoComplete="new-password"
            required
            minLength={MIN_PASSWORD_LENGTH}
            maxLength={MAX_PASSWORD_LENGTH}
            className={INPUT_CLASSES}
          />
          <p className={`mt-1 ${MUTED_TEXT_CLASSES}`}>
            Entre {MIN_PASSWORD_LENGTH} y {MAX_PASSWORD_LENGTH} caracteres.
          </p>
        </div>

        <div>
          <label htmlFor="passwordConfirmation" className={LABEL_CLASSES}>
            Repetir contraseña
          </label>
          <input
            id="passwordConfirmation"
            name="passwordConfirmation"
            type="password"
            autoComplete="new-password"
            required
            className={INPUT_CLASSES}
          />
        </div>

        {message && (
          <p role="alert" className={`sm:col-span-2 ${ERROR_CLASSES}`}>
            {message}
          </p>
        )}

        <button
          type="submit"
          disabled={isLoading}
          className={`w-full sm:col-span-2 ${PRIMARY_BUTTON_CLASSES}`}
        >
          {isLoading ? 'Creando cuenta...' : 'Crear cuenta'}
        </button>
      </form>

      <p className={`mt-6 text-center ${MUTED_TEXT_CLASSES}`}>
        ¿Ya tenés cuenta?{' '}
        <Link to={`/${slug}/login`} className={LINK_CLASSES}>
          Iniciá sesión
        </Link>
      </p>
    </AuthCard>
  )
}
