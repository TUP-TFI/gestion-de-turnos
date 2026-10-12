import { Navigate, Outlet } from 'react-router'
import { useAuth } from '../hooks/useAuth'
import { getDashboardPath } from '../lib/panelRoutes'
import type { UserRole } from '../types/User'

interface ProtectedRouteProps {
  allowedRole: UserRole
}

export function ProtectedRoute({ allowedRole }: ProtectedRouteProps) {
  const { session } = useAuth()

  if (!session) {
    return <Navigate to="/login" replace />
  }

  if (session.user.role !== allowedRole) {
    return <Navigate to={getDashboardPath(session.user.role) ?? '/login'} replace />
  }

  return <Outlet />
}
