import { useEffect, useRef, useState } from 'react'
import type { InputHTMLAttributes } from 'react'
import { INPUT_BASE_CLASSES } from '../lib/formStyles'

// Los mismos props que un <input>, salvo el tipo, que lo maneja el botón. Sirve con value y onChange (LoginPage) y con
// name y FormData (ClientLoginPage).
type PasswordInputProps = Omit<
  InputHTMLAttributes<HTMLInputElement>,
  'type' | 'className'
>

// Campo de contraseña con un botón de ojo para mostrar y ocultar lo que se escribió (T-13.2). Cada campo tiene su
// propio botón: "Repetir contraseña" se muestra aparte.
export function PasswordInput(props: PasswordInputProps) {
  const [isVisible, setIsVisible] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)

  // Al enviar vuelve a ocultarse: algunos gestores de contraseñas no ofrecen guardarla si el campo es de texto.
  useEffect(() => {
    const form = inputRef.current?.form
    if (!form) {
      return
    }
    const hide = () => setIsVisible(false)
    form.addEventListener('submit', hide)
    return () => form.removeEventListener('submit', hide)
  }, [])

  return (
    <div className="relative mt-1">
      <input
        {...props}
        ref={inputRef}
        type={isVisible ? 'text' : 'password'}
        // pr-10 deja lugar al botón. Se oculta el ojo nativo de Edge para que no queden dos.
        className={`${INPUT_BASE_CLASSES} pr-10 [&::-ms-reveal]:hidden`}
      />
      <button
        type="button"
        onClick={() => setIsVisible((visible) => !visible)}
        aria-label={isVisible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
        aria-pressed={isVisible}
        className="absolute inset-y-0 right-0 flex w-10 cursor-pointer items-center justify-center rounded-r-lg text-muted focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-light"
      >
        {isVisible ? <EyeOffIcon /> : <EyeIcon />}
      </button>
    </div>
  )
}

function EyeIcon() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      className="h-5 w-5"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  )
}

function EyeOffIcon() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      className="h-5 w-5"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M10.6 5.1A10.4 10.4 0 0 1 12 5c6.5 0 10 7 10 7a17.6 17.6 0 0 1-2.2 3.2" />
      <path d="M6.6 6.6C3.6 8.6 2 12 2 12s3.5 7 10 7a9.7 9.7 0 0 0 5.4-1.6" />
      <path d="M9.9 9.9a3 3 0 0 0 4.2 4.2" />
      <path d="M2 2l20 20" />
    </svg>
  )
}
