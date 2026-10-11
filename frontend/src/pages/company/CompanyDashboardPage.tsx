import { useAuth } from '../../hooks/useAuth'

export function CompanyDashboardPage() {
  const { session, logout } = useAuth()

  return (
    <main className="min-h-screen bg-slate-50 px-4 py-8">
      <div className="mx-auto max-w-3xl">
        <h1 className="text-2xl font-semibold text-slate-900">
          Panel de empresa
        </h1>
        <p className="mt-1 text-sm text-slate-600">
          Hola, {session?.user.firstName}
        </p>
        <button
          type="button"
          onClick={logout}
          className="mt-6 cursor-pointer rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 focus:outline-none focus:ring-2 focus:ring-brand-light"
        >
          Cerrar sesión
        </button>
      </div>
    </main>
  )
}
