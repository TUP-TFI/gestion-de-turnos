import { useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { ClientAuthContext } from './ClientAuthContext'
import {
  clearClientSession,
  onClientSessionChange,
  readClientSession,
  saveClientSession,
} from '../lib/clientSession'
import type { ClientSession } from '../types/Auth'

interface ClientAuthProviderProps {
  children: ReactNode
}

// Sesión del cliente final, separada de la del panel (AuthProvider).
export function ClientAuthProvider({ children }: ClientAuthProviderProps) {
  // El valor inicial sale de localStorage, así la sesión sobrevive a recargar la página.
  const [session, setSession] = useState<ClientSession | null>(
    readClientSession,
  )
  const queryClient = useQueryClient()

  // Se guarda una sola sesión de cliente: si otra pestaña entra en otra empresa o cierra la sesión, esta se entera
  // sin recargar. La caché se limpia por lo mismo que en logout.
  useEffect(
    () =>
      onClientSessionChange((newSession) => {
        setSession(newSession)
        queryClient.clear()
      }),
    [queryClient],
  )

  function startSession(newSession: ClientSession) {
    saveClientSession(newSession)
    setSession(newSession)
  }

  function logout() {
    clearClientSession()
    setSession(null)
    // Sin esto, el próximo cliente que entre en este navegador vería datos cacheados de la sesión anterior.
    queryClient.clear()
  }

  return (
    <ClientAuthContext.Provider value={{ session, startSession, logout }}>
      {children}
    </ClientAuthContext.Provider>
  )
}
