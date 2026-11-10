import { Link } from 'react-router'
import { AppointmentStatusBadge } from '../../components/AppointmentStatusBadge'
import { useAppointmentSummary } from '../../hooks/useAppointmentSummary'
import { useAuth } from '../../hooks/useAuth'
import { useBranding } from '../../hooks/useBranding'
import { formatTimeInZone } from '../../lib/companyTime'
import { formatInCompanyZone } from '../../lib/formatInCompanyZone'
import {
  CARD_TITLE_CLASSES,
  ERROR_CLASSES,
  LINK_CLASSES,
  MUTED_TEXT_CLASSES,
  TABLE_CELL_CLASSES,
  TABLE_HEAD_CLASSES,
  TABLE_WRAPPER_CLASSES,
} from '../../lib/formStyles'
import type { AdminAppointment } from '../../types/Appointment'

interface AppointmentBlockProps {
  title: string
  emptyText: string
  appointments: AdminAppointment[]
  timezone: string
  // Los de hoy muestran solo la hora; los próximos, fecha y hora.
  showDate: boolean
}

function AppointmentBlock({
  title,
  emptyText,
  appointments,
  timezone,
  showDate,
}: AppointmentBlockProps) {
  return (
    <section className="space-y-3">
      <h3 className={CARD_TITLE_CLASSES}>{title}</h3>

      {appointments.length === 0 ? (
        <p className={`${MUTED_TEXT_CLASSES} text-center`}>{emptyText}</p>
      ) : (
        <div className={TABLE_WRAPPER_CLASSES}>
          <table className="w-full text-left text-sm text-body">
            <thead className={TABLE_HEAD_CLASSES}>
              <tr>
                <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                  {showDate ? 'Fecha y hora' : 'Hora'}
                </th>
                <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                  Servicio
                </th>
                <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                  Cliente
                </th>
                <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                  Teléfono
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
                    {showDate
                      ? formatInCompanyZone(appointment.startDateTime, timezone)
                      : formatTimeInZone(appointment.startDateTime, timezone)}
                  </td>
                  <td className={`${TABLE_CELL_CLASSES} font-medium text-ink`}>
                    {appointment.serviceName}
                  </td>
                  <td className={TABLE_CELL_CLASSES}>{appointment.client.name}</td>
                  <td className={`${TABLE_CELL_CLASSES} whitespace-nowrap`}>
                    {appointment.client.phone ?? '-'}
                  </td>
                  <td className={TABLE_CELL_CLASSES}>
                    <AppointmentStatusBadge appointment={appointment} />
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

// Inicio del panel: saludo y los turnos de hoy y los próximos (US-09.1). Las horas van en la zona de la empresa; sin
// ella no se muestra la tabla, para no mostrar horas equivocadas.
export function CompanyDashboardPage() {
  const { session } = useAuth()
  const {
    branding,
    error: brandingError,
    isLoading: isLoadingBranding,
  } = useBranding()
  const {
    summary,
    error: summaryError,
    isLoading: isLoadingSummary,
  } = useAppointmentSummary()

  const error = brandingError ?? summaryError
  const isLoading = isLoadingBranding || isLoadingSummary

  return (
    <section className="space-y-6">
      <p className={MUTED_TEXT_CLASSES}>Hola, {session?.user.firstName}</p>

      {isLoading && <p className={MUTED_TEXT_CLASSES}>Cargando...</p>}

      {error && (
        <p role="alert" className={ERROR_CLASSES}>
          {error}
        </p>
      )}

      {branding && summary && (
        <>
          <AppointmentBlock
            title="Hoy"
            emptyText="No hay turnos para hoy"
            appointments={summary.today}
            timezone={branding.timezone}
            showDate={false}
          />
          <AppointmentBlock
            title="Próximos turnos"
            emptyText="No hay próximos turnos"
            appointments={summary.upcoming}
            timezone={branding.timezone}
            showDate
          />
          <Link to="/company/appointments" className={LINK_CLASSES}>
            Ver todos los turnos
          </Link>
        </>
      )}
    </section>
  )
}
