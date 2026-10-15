import type {
  ActivationRequest,
  LoginRequest,
  LoginResponse,
} from '../types/Auth'
import { request } from './client'

export function login(body: LoginRequest): Promise<LoginResponse> {
  return request<LoginResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(body),
    auth: false,
  })
}

// El backend responde 204 sin cuerpo: después de activar se entra por el login.
export function activateAccount(body: ActivationRequest): Promise<void> {
  return request<void>('/api/auth/activate', {
    method: 'POST',
    body: JSON.stringify(body),
    auth: false,
  })
}
