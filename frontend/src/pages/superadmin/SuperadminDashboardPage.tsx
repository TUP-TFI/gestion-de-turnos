import { useAuth } from '../../hooks/useAuth'

export function SuperadminDashboardPage() {
  const { session } = useAuth()

  return (
    <p className="text-sm text-slate-600">Hola, {session?.user.firstName}</p>
  )
}
