import type { LoginResponse } from '../types/Auth'

const STORAGE_KEY = 'panel_session'

export function savePanelSession(session: LoginResponse): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

export function readPanelSession(): LoginResponse | null {
  const stored = localStorage.getItem(STORAGE_KEY)
  if (!stored) {
    return null
  }

  try {
    return JSON.parse(stored)
  } catch {
    // Valor dañado: se descarta para que la app arranque sin sesión en vez de romperse.
    clearPanelSession()
    return null
  }
}

export function clearPanelSession(): void {
  localStorage.removeItem(STORAGE_KEY)
}
