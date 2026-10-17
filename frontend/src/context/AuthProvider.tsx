import { useState } from 'react'
import type { ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { AuthContext } from './AuthContext'
import {
  clearPanelSession,
  readPanelSession,
  savePanelSession,
} from '../lib/panelSession'
import type { LoginResponse } from '../types/Auth'

interface AuthProviderProps {
  children: ReactNode
}

export function AuthProvider({ children }: AuthProviderProps) {
  // El valor inicial sale de localStorage, así la sesión sobrevive a recargar la página.
  const [session, setSession] = useState<LoginResponse | null>(readPanelSession)
  const queryClient = useQueryClient()

  function startSession(newSession: LoginResponse) {
    savePanelSession(newSession)
    setSession(newSession)
  }

  function logout() {
    clearPanelSession()
    setSession(null)
    // Sin esto, el próximo usuario que entre en este navegador vería datos cacheados de la sesión anterior.
    queryClient.clear()
  }

  return (
    <AuthContext.Provider value={{ session, startSession, logout }}>
      {children}
    </AuthContext.Provider>
  )
}
