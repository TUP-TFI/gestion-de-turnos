import type { LoginRequest, LoginResponse } from '../types/Auth'
import { request } from './client'

export function login(body: LoginRequest): Promise<LoginResponse> {
  return request<LoginResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(body),
    auth: false,
  })
}
