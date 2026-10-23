import { useEffect } from 'react'
import { useNavigate } from 'react-router'

// Tiempo para leer el aviso de éxito antes de volver al listado.
const REDIRECT_DELAY_MS = 2000

// Después de guardar bien (done = true), espera unos segundos y navega a `to`. Si se sale antes de la
// pantalla, el temporizador se cancela.
export function useRedirectAfterSuccess(done: boolean, to: string): void {
  const navigate = useNavigate()

  useEffect(() => {
    if (!done) {
      return
    }
    const timer = setTimeout(() => navigate(to), REDIRECT_DELAY_MS)
    return () => clearTimeout(timer)
  }, [done, to, navigate])
}
