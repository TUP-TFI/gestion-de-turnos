import type { ReactNode } from 'react'
import { MUTED_TEXT_CLASSES, PAGE_TITLE_CLASSES } from '../lib/formStyles'

interface AuthCardProps {
  title: string
  subtitle: string
  children: ReactNode
}

// Tarjeta centrada de las pantallas sin sesión (login, activación de cuenta).
export function AuthCard({ title, subtitle, children }: AuthCardProps) {
  return (
    <main className="flex min-h-screen items-center justify-center bg-surface px-4 py-8">
      <div className="w-full max-w-sm rounded-xl border border-hairline bg-white p-6 shadow-sm sm:p-8">
        <h1 className={`text-center ${PAGE_TITLE_CLASSES}`}>
          {title}
        </h1>
        <p className={`mt-1 text-center ${MUTED_TEXT_CLASSES}`}>{subtitle}</p>
        {children}
      </div>
    </main>
  )
}
