import { readPanelSession } from '../lib/panelSession'
import type { ApiError } from '../types/Auth'

const API_URL = import.meta.env.VITE_API_URL

export class ApiRequestError extends Error {
  code: string

  constructor(error: ApiError) {
    super(error.message)
    this.code = error.code
  }
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

  if (!response.ok) {
    throw new ApiRequestError(await readError(response))
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
