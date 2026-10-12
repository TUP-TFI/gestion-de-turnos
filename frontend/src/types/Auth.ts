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

// Forma de los errores del backend: code para distinguir casos, message para mostrar.
export interface ApiError {
  code: string
  message: string
}
