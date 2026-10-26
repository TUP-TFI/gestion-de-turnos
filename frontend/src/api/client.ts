import { clearPanelSession, readPanelSession } from '../lib/panelSession'
import type { ApiError } from '../types/Auth'

const API_URL = import.meta.env.VITE_API_URL

export class ApiRequestError extends Error {
  code: string

  constructor(error: ApiError) {
    super(error.message)
    this.code = error.code
  }
}

// Texto para mostrarle al usuario: el del backend si lo hay, o uno genérico.
export function toErrorMessage(error: Error): string {
  return error instanceof ApiRequestError
    ? error.message
    : 'Ocurrió un error inesperado. Intentá de nuevo.'
}

interface RequestOptions extends RequestInit {
  // false en los endpoints sin sesión (login, activación): no se manda el token.
  auth?: boolean
}

// Único punto que habla con el backend: arma la URL, agrega el token y traduce los errores.
export async function request<T>(
  path: string,
  { auth = true, ...init }: RequestOptions = {},
): Promise<T> {
  const headers = new Headers(init.headers)
  if (init.body) {
    headers.set('Content-Type', 'application/json')
  }
  const token = auth ? readPanelSession()?.token : undefined
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  let response: Response
  try {
    response = await fetch(`${API_URL}${path}`, { ...init, headers })
  } catch {
    throw new ApiRequestError({
      code: 'NETWORK_ERROR',
      message: 'No se pudo conectar con el servidor. Intentá de nuevo.',
    })
  }

  // 401 en un pedido con sesión: el token venció o no es válido. Se cierra la sesión y se vuelve al login.
  if (response.status === 401 && token) {
    clearPanelSession()
    window.location.assign('/login')
  }

  if (!response.ok) {
    const error = await readError(response)
    // COMPANY_INACTIVE en un pedido con sesión: la empresa se desactivó con la sesión abierta (US-04.4).
    if (error.code === 'COMPANY_INACTIVE' && token) {
      clearPanelSession()
      window.location.assign('/login?reason=company-inactive')
    }
    throw new ApiRequestError(error)
  }

  // 204: respuesta correcta sin cuerpo.
  if (response.status === 204) {
    return undefined as T
  }

  return response.json()
}

async function readError(response: Response): Promise<ApiError> {
  try {
    return await response.json()
  } catch {
    return {
      code: 'UNKNOWN_ERROR',
      message: 'Ocurrió un error inesperado. Intentá de nuevo.',
    }
  }
}
