import type { User } from './User'

export interface LoginRequest {
  email: string
  password: string
}

// Los campos de User que devuelve el login.
export type SessionUser = Pick<
  User,
  'id' | 'firstName' | 'lastName' | 'email' | 'role' | 'companyId'
>

export interface LoginResponse {
  token: string
  user: SessionUser
}

// Cuerpo de POST /api/public/companies/{slug}/register. Responde lo mismo que el login.
export type ClientRegisterRequest = Pick<
  User,
  'firstName' | 'lastName' | 'email' | 'phone'
> & { password: string }

// Sesión del cliente. Guarda el slug porque la cuenta sirve solo en esa empresa.
export interface ClientSession extends LoginResponse {
  slug: string
}

// token es el valor que llega en el link de activación.
export interface ActivationRequest {
  token: string
  password: string
}

// Forma de los errores del backend: code para distinguir casos, message para mostrar.
export interface ApiError {
  code: string
  message: string
}
