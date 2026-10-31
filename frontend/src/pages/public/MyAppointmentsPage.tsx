import { useState } from 'react'
import { Navigate, useParams } from 'react-router'
import { useClientAppointments } from '../../hooks/useClientAppointments'
import { useClientAuth } from '../../hooks/useClientAuth'
import { usePublicCompany } from '../../hooks/usePublicCompany'
import { formatInCompanyZone } from '../../lib/formatInCompanyZone'
import {
  BADGE_CLASSES,
  BADGE_VARIANT_CLASSES,
  ERROR_CLASSES,
  INPUT_CLASSES,
  LABEL_CLASSES,
  MUTED_TEXT_CLASSES,
  PAGE_TITLE_CLASSES,
  TABLE_CELL_CLASSES,
  TABLE_HEAD_CLASSES,
  TABLE_WRAPPER_CLASSES,
} from '../../lib/formStyles'
import type { AppointmentStatus } from '../../types/Appointment'

const STATUS_LABELS: Record<AppointmentStatus, string> = {
  PENDING: 'Pendiente',
  COMPLETED: 'Completado',
  CANCELLED: 'Cancelado',
}

const STATUS_CLASSES: Record<AppointmentStatus, string> = {
  PENDING: BADGE_VARIANT_CLASSES.warning,
  COMPLETED: BADGE_VARIANT_CLASSES.success,
  CANCELLED: BADGE_VARIANT_CLASSES.neutral,
}

// Turnos del cliente en esta empresa (US-07.6). Los COMPLETED cumplen la función de historial.
export function MyAppointmentsPage() {
  const { slug = '' } = useParams()
  const { session } = useClientAuth(slug)
  const {
    company,
    error: companyError,
    isLoading: isLoadingCompany,
  } = usePublicCompany(slug)
  const [status, setStatus] = useState<AppointmentStatus | ''>('')
  const {
    appointments,
    error: appointmentsError,
    isLoading: isLoadingAppointments,
  } = useClientAppointments(slug, status || undefined)

  if (!session) {
    return <Navigate to={`/${slug}/login`} replace />
  }

  const error = companyError ?? appointmentsError
  const isLoading = isLoadingCompany || isLoadingAppointments

  return (
    <main className="min-h-screen bg-surface px-4 py-8">
      <section className="mx-auto max-w-4xl space-y-6">
        <div>
          <h1 className={PAGE_TITLE_CLASSES}>Mis turnos</h1>
          {company && <p className={MUTED_TEXT_CLASSES}>{company.name}</p>}
        </div>

        <div className="sm:w-56">
          <label htmlFor="status" className={LABEL_CLASSES}>
            Estado
          </label>
          <select
            id="status"
            value={status}
            onChange={(event) =>
              setStatus(event.target.value as AppointmentStatus | '')
            }
            className={INPUT_CLASSES}
          >
            <option value="">Todos</option>
            <option value="PENDING">{STATUS_LABELS.PENDING}</option>
            <option value="COMPLETED">{STATUS_LABELS.COMPLETED}</option>
            <option value="CANCELLED">{STATUS_LABELS.CANCELLED}</option>
          </select>
        </div>

        {isLoading && <p className={MUTED_TEXT_CLASSES}>Cargando...</p>}

        {error && (
          <p role="alert" className={ERROR_CLASSES}>
            {error}
          </p>
        )}

        {!isLoading && !error && appointments.length === 0 && (
          <p className={`${MUTED_TEXT_CLASSES} text-center`}>
            {status ? 'No tenés turnos en este estado' : 'Todavía no tenés turnos'}
          </p>
        )}

        {/* La hora se muestra en la zona de la empresa: sin la empresa todavía no se puede convertir. */}
        {company && appointments.length > 0 && (
          <div className={TABLE_WRAPPER_CLASSES}>
            <table className="w-full text-left text-sm text-body">
              <thead className={TABLE_HEAD_CLASSES}>
                <tr>
                  <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                    Fecha y hora
                  </th>
                  <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                    Servicio
                  </th>
                  <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                    Estado
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-hairline">
                {appointments.map((appointment) => (
                  <tr key={appointment.id}>
                    <td className={`${TABLE_CELL_CLASSES} whitespace-nowrap`}>
                      {formatInCompanyZone(
                        appointment.startDateTime,
                        company.timezone,
                      )}
                    </td>
                    <td className={`${TABLE_CELL_CLASSES} font-medium text-ink`}>
                      {appointment.serviceName}
                    </td>
                    <td className={TABLE_CELL_CLASSES}>
                      <div className="flex flex-wrap gap-2">
                        <span
                          className={`${BADGE_CLASSES} ${STATUS_CLASSES[appointment.status]}`}
                        >
                          {STATUS_LABELS[appointment.status]}
                        </span>
                        {appointment.rescheduleCount > 0 && (
                          <span
                            className={`${BADGE_CLASSES} ${BADGE_VARIANT_CLASSES.neutral}`}
                          >
                            Reprogramado
                          </span>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </main>
  )
}
