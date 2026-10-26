import { Link } from 'react-router'
import { useDeleteScheduleException } from '../../hooks/useDeleteScheduleException'
import { useScheduleExceptions } from '../../hooks/useScheduleExceptions'
import {
  BADGE_CLASSES,
  BADGE_VARIANT_CLASSES,
  ERROR_CLASSES,
  LINK_CLASSES,
  MUTED_TEXT_CLASSES,
  PAGE_TITLE_CLASSES,
  PRIMARY_BUTTON_CLASSES,
  TABLE_CELL_CLASSES,
  TABLE_HEAD_CLASSES,
  TABLE_WRAPPER_CLASSES,
} from '../../lib/formStyles'
import { formatDate } from '../../lib/scheduleExceptionForm'
import type { ScheduleException } from '../../types/ScheduleException'

// El backend manda '10:00:00': se muestra '10:00 a 14:00'.
function hoursLabel(exception: ScheduleException): string {
  return `${exception.startTime?.slice(0, 5)} a ${exception.endTime?.slice(0, 5)}`
}

export function ScheduleExceptionsPage() {
  const { exceptions, error, isLoading } = useScheduleExceptions()
  const {
    deleteException,
    error: deleteError,
    isLoading: isDeleting,
  } = useDeleteScheduleException()

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="space-y-1">
          <h2 className={PAGE_TITLE_CLASSES}>Feriados y días especiales</h2>
          <p className={MUTED_TEXT_CLASSES}>
            En estas fechas no se usa el horario semanal: el día queda cerrado o con el horario que cargues.
          </p>
        </div>
        <Link to="/company/schedule-exceptions/new" className={PRIMARY_BUTTON_CLASSES}>
          Nueva excepción
        </Link>
      </div>

      {isLoading && <p className={MUTED_TEXT_CLASSES}>Cargando...</p>}

      {error && (
        <p role="alert" className={ERROR_CLASSES}>
          {error}
        </p>
      )}

      {deleteError && (
        <p role="alert" className={ERROR_CLASSES}>
          {deleteError}
        </p>
      )}

      {!isLoading && !error && exceptions.length === 0 && (
        <p className={`${MUTED_TEXT_CLASSES} text-center`}>
          No hay feriados ni días especiales cargados
        </p>
      )}

      {exceptions.length > 0 && (
        <div className={TABLE_WRAPPER_CLASSES}>
          <table className="w-full text-left text-sm text-body">
            <thead className={TABLE_HEAD_CLASSES}>
              <tr>
                <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                  Fecha
                </th>
                <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                  Horario
                </th>
                <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                  Motivo
                </th>
                <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                  Acciones
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-hairline">
              {exceptions.map((exception) => (
                <tr key={exception.id}>
                  <td className={`${TABLE_CELL_CLASSES} whitespace-nowrap font-medium text-ink`}>
                    {formatDate(exception.exceptionDate)}
                  </td>
                  <td className={`${TABLE_CELL_CLASSES} whitespace-nowrap`}>
                    {exception.isClosed ? (
                      <span className={`${BADGE_CLASSES} ${BADGE_VARIANT_CLASSES.neutral}`}>
                        Cerrado
                      </span>
                    ) : (
                      hoursLabel(exception)
                    )}
                  </td>
                  <td className={TABLE_CELL_CLASSES}>{exception.reason ?? '—'}</td>
                  <td className={`${TABLE_CELL_CLASSES} whitespace-nowrap`}>
                    <div className="flex gap-4">
                      <Link
                        to={`/company/schedule-exceptions/${exception.id}/edit`}
                        className={LINK_CLASSES}
                      >
                        Editar
                      </Link>
                      <button
                        type="button"
                        onClick={() => deleteException(exception)}
                        disabled={isDeleting}
                        className={`${LINK_CLASSES} cursor-pointer disabled:cursor-not-allowed disabled:text-muted`}
                      >
                        Borrar
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
