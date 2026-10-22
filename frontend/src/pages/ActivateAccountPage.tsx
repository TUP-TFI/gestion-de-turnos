import { useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { AuthCard } from '../components/AuthCard'
import { useActivateAccount } from '../hooks/useActivateAccount'
import {
  ERROR_CLASSES,
  INPUT_CLASSES,
  LABEL_CLASSES,
  MUTED_TEXT_CLASSES,
  PRIMARY_BUTTON_CLASSES,
} from '../lib/formStyles'

// Los mismos límites que valida el backend.
const MIN_PASSWORD_LENGTH = 8
const MAX_PASSWORD_LENGTH = 72

export function ActivateAccountPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const { activateAccount, error, isLoading, isActivated } =
    useActivateAccount()
  const [password, setPassword] = useState('')
  const [passwordConfirmation, setPasswordConfirmation] = useState('')
  const [wasSubmitted, setWasSubmitted] = useState(false)

  if (!token) {
    return (
      <AuthCard
        title="Activá tu cuenta"
        subtitle="El link de activación no es válido"
      >
        <p className={`mt-6 text-center ${MUTED_TEXT_CLASSES}`}>
          Pedile al administrador de la plataforma que te envíe uno nuevo.
        </p>
      </AuthCard>
    )
  }

  if (isActivated) {
    return (
      <AuthCard
        title="Tu cuenta quedó activada"
        subtitle="Ya podés ingresar con tu email y tu contraseña"
      >
        <Link to="/login" className={`mt-6 ${PRIMARY_BUTTON_CLASSES}`}>
          Iniciar sesión
        </Link>
      </AuthCard>
    )
  }

  const passwordsMismatch = wasSubmitted && password !== passwordConfirmation
  const message = passwordsMismatch ? 'Las contraseñas no coinciden' : error

  return (
    <AuthCard title="Activá tu cuenta" subtitle="Definí tu contraseña">
      <form
        className="mt-6 space-y-4"
        onSubmit={(event) => {
          event.preventDefault()
          setWasSubmitted(true)
          if (password === passwordConfirmation) {
            activateAccount({ token, password })
          }
        }}
      >
        <div>
          <label htmlFor="password" className={LABEL_CLASSES}>
            Contraseña
          </label>
          <input
            id="password"
            type="password"
            autoComplete="new-password"
            required
            minLength={MIN_PASSWORD_LENGTH}
            maxLength={MAX_PASSWORD_LENGTH}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
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
            type="password"
            autoComplete="new-password"
            required
            value={passwordConfirmation}
            onChange={(event) => setPasswordConfirmation(event.target.value)}
            className={INPUT_CLASSES}
          />
        </div>

        {message && (
          <p role="alert" className={ERROR_CLASSES}>
            {message}
          </p>
        )}

        <button
          type="submit"
          disabled={isLoading}
          className={PRIMARY_BUTTON_CLASSES}
        >
          {isLoading ? 'Activando...' : 'Activar cuenta'}
        </button>
      </form>
    </AuthCard>
  )
}
