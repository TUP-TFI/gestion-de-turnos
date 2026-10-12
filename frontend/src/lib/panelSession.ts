import type { LoginResponse } from '../types/Auth'

const STORAGE_KEY = 'panel_session'

export function savePanelSession(session: LoginResponse): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

export function readPanelSession(): LoginResponse | null {
  const stored = localStorage.getItem(STORAGE_KEY)
  return stored ? JSON.parse(stored) : null
}

export function clearPanelSession(): void {
  localStorage.removeItem(STORAGE_KEY)
}
