import { DAY_OF_WEEK_LABELS, DAYS_OF_WEEK } from '../lib/daysOfWeek'
import {
  ERROR_CLASSES,
  MUTED_TEXT_CLASSES,
  PAGE_TITLE_CLASSES,
} from '../lib/formStyles'
import type { PublicBusinessHours } from '../types/PublicCompany'

interface CompanyBusinessHoursProps {
  hours: PublicBusinessHours[]
  error: string | null
  isLoading: boolean
}

// El backend manda 'HH:mm:ss'; se muestra 'HH:mm'.
function toHourMinute(time: string): string {
  return time.slice(0, 5)
}

// Horario semanal de la página pública. Las horas ya son locales de la empresa: no se convierten.
export function CompanyBusinessHours({
  hours,
  error,
  isLoading,
}: CompanyBusinessHoursProps) {
  if (isLoading) {
    return null
  }

  return (
    <section className="space-y-4">
      <h2 className={PAGE_TITLE_CLASSES}>Horario de atención</h2>

      {error ? (
        <p role="alert" className={ERROR_CLASSES}>
          {error}
        </p>
      ) : hours.length === 0 ? (
        <p className={MUTED_TEXT_CLASSES}>Todavía no hay horario cargado</p>
      ) : (
        <dl className="max-w-md divide-y divide-hairline rounded-xl border border-hairline bg-white">
          {DAYS_OF_WEEK.map((day) => {
            // Las franjas llegan ordenadas por día y hora de inicio: una jornada partida queda en orden.
            const ranges = hours
              .filter((item) => item.dayOfWeek === day)
              .map(
                (item) =>
                  `${toHourMinute(item.startTime)}–${toHourMinute(item.endTime)}`,
              )

            return (
              <div
                key={day}
                className="flex flex-wrap justify-between gap-x-4 px-4 py-2 text-sm"
              >
                <dt className="font-medium text-ink">
                  {DAY_OF_WEEK_LABELS[day]}
                </dt>
                <dd className={ranges.length > 0 ? 'text-body' : 'text-muted'}>
                  {ranges.length > 0 ? ranges.join(' · ') : 'Cerrado'}
                </dd>
              </div>
            )
          })}
        </dl>
      )}
    </section>
  )
}
