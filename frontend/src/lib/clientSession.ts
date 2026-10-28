import type { ClientSession } from '../types/Auth'

// Distinta de la del panel: un admin y un cliente pueden tener sesión abierta en el mismo navegador.
const STORAGE_KEY = 'client_session'

export function saveClientSession(session: ClientSession): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

export function readClientSession(): ClientSession | null {
  const stored = localStorage.getItem(STORAGE_KEY)
  if (!stored) {
    return null
  }

  try {
    return JSON.parse(stored)
  } catch {
    // Valor dañado: se descarta para que la app arranque sin sesión en vez de romperse.
    clearClientSession()
    return null
  }
}

export function clearClientSession(): void {
  localStorage.removeItem(STORAGE_KEY)
}

// Avisa cuando otra pestaña inicia o cierra la sesión del cliente. El evento storage no llega a la pestaña que hizo
// el cambio, solo a las demás. Devuelve la función para dejar de escuchar.
export function onClientSessionChange(
  listener: (session: ClientSession | null) => void,
): () => void {
  function handleStorage(event: StorageEvent) {
    // key es null cuando se borra todo el localStorage.
    if (event.key === STORAGE_KEY || event.key === null) {
      listener(readClientSession())
    }
  }

  window.addEventListener('storage', handleStorage)
  return () => window.removeEventListener('storage', handleStorage)
}
