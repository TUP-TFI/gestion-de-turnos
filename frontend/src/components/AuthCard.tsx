import type { ReactNode } from 'react'

interface AuthCardProps {
  title: string
  subtitle: string
  children: ReactNode
}

// Tarjeta centrada de las pantallas sin sesión (login, activación de cuenta).
export function AuthCard({ title, subtitle, children }: AuthCardProps) {
  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-50 px-4 py-8">
      <div className="w-full max-w-sm rounded-xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <h1 className="text-center text-2xl font-semibold text-slate-900">
          {title}
        </h1>
        <p className="mt-1 text-center text-sm text-slate-600">{subtitle}</p>
        {children}
      </div>
    </main>
  )
}
