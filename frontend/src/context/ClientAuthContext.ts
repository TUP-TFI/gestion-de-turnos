import { createContext } from 'react'
import type { ClientSession } from '../types/Auth'

export interface ClientAuthContextValue {
  // null cuando no hay sesión de cliente iniciada.
  session: ClientSession | null
  startSession: (session: ClientSession) => void
  logout: () => void
}

export const ClientAuthContext = createContext<ClientAuthContextValue | null>(
  null,
)
