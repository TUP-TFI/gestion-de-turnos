import type { ApiError, LoginRequest, LoginResponse } from '../types/Auth'

const API_URL = import.meta.env.VITE_API_URL

export class ApiRequestError extends Error {
  code: string

  constructor(error: ApiError) {
    super(error.message)
    this.code = error.code
  }
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
  let response: Response
  try {
    response = await fetch(`${API_URL}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    })
  } catch {
    throw new ApiRequestError({
      code: 'NETWORK_ERROR',
      message: 'No se pudo conectar con el servidor. Intentá de nuevo.',
    })
  }

  if (!response.ok) {
    throw new ApiRequestError(await response.json())
  }

  return response.json()
}
