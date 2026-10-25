import { useAuth } from '../../hooks/useAuth'
import { MUTED_TEXT_CLASSES } from '../../lib/formStyles'

export function CompanyDashboardPage() {
  const { session } = useAuth()

  return (
    <p className={MUTED_TEXT_CLASSES}>Hola, {session?.user.firstName}</p>
  )
}
