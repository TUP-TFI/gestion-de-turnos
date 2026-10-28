import { useContext } from 'react'
import { ClientAuthContext } from '../context/ClientAuthContext'
import type { ClientAuthContextValue } from '../context/ClientAuthContext'

// La sesión solo cuenta en la empresa donde se inició: en otro slug, el cliente figura sin sesión.
export function useClientAuth(slug: string): ClientAuthContextValue {
  const value = useContext(ClientAuthContext)

  if (value === null) {
    throw new Error(
      'useClientAuth se tiene que usar dentro de <ClientAuthProvider>',
    )
  }

  const session = value.session?.slug === slug ? value.session : null
  return { ...value, session }
}
