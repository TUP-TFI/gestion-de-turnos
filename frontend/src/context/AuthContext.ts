import { createContext } from 'react'
import type { LoginResponse } from '../types/Auth'

export interface AuthContextValue {
  // null cuando no hay sesión iniciada.
  session: LoginResponse | null
  startSession: (session: LoginResponse) => void
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
