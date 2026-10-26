import { useAuth } from '../../hooks/useAuth'
import { useSuperadminStats } from '../../hooks/useSuperadminStats'
import {
  CARD_TITLE_CLASSES,
  ERROR_CLASSES,
  FIELDSET_CLASSES,
  MUTED_TEXT_CLASSES,
} from '../../lib/formStyles'

const NUMBER_FORMAT = new Intl.NumberFormat('es-AR')

export function SuperadminDashboardPage() {
  const { session } = useAuth()
  const { stats, error, isLoading } = useSuperadminStats()

  return (
    <section className="space-y-6">
      <p className={MUTED_TEXT_CLASSES}>Hola, {session?.user.firstName}</p>

      {isLoading && <p className={MUTED_TEXT_CLASSES}>Cargando...</p>}

      {error && (
        <p role="alert" className={ERROR_CLASSES}>
          {error}
        </p>
      )}

      {stats && (
        <div className="grid gap-4 sm:grid-cols-2">
          <div className={FIELDSET_CLASSES}>
            <h2 className={CARD_TITLE_CLASSES}>Empresas</h2>
            <dl className="grid grid-cols-2 gap-4">
              <Indicator label="Activas" value={stats.activeCompanies} />
              <Indicator label="Inactivas" value={stats.inactiveCompanies} />
            </dl>
          </div>

          <div className={FIELDSET_CLASSES}>
            <h2 className={CARD_TITLE_CLASSES}>Turnos</h2>
            <dl>
              <Indicator label="Total" value={stats.totalAppointments} />
            </dl>
          </div>
        </div>
      )}
    </section>
  )
}

function Indicator({ label, value }: { label: string; value: number }) {
  return (
    <div>
      <dt className={MUTED_TEXT_CLASSES}>{label}</dt>
      <dd className="text-[22px] font-semibold text-ink">{NUMBER_FORMAT.format(value)}</dd>
    </div>
  )
}
