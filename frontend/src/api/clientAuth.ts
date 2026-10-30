import type {
  ClientRegisterRequest,
  LoginRequest,
  LoginResponse,
} from '../types/Auth'
import { request } from './client'

// Registro y login del cliente, desde la página pública de una empresa. Todavía no hay sesión.
export function registerClient(
  slug: string,
  body: ClientRegisterRequest,
): Promise<LoginResponse> {
  return request<LoginResponse>(`/api/public/companies/${slug}/register`, {
    method: 'POST',
    body: JSON.stringify(body),
    auth: false,
  })
}

export function loginClient(
  slug: string,
  body: LoginRequest,
): Promise<LoginResponse> {
  return request<LoginResponse>(`/api/public/companies/${slug}/login`, {
    method: 'POST',
    body: JSON.stringify(body),
    auth: false,
  })
}
