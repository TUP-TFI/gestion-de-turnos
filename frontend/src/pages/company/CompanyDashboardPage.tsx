import { Link } from 'react-router'
import { AdminCancelAppointmentModal } from '../../components/AdminCancelAppointmentModal'
import { CancelNoticeContent } from '../../components/CancelNoticeContent'
import { AppointmentStatusBadge } from '../../components/AppointmentStatusBadge'
import { DismissibleNotice } from '../../components/DismissibleNotice'
import { useAppointmentSummary } from '../../hooks/useAppointmentSummary'
import { useAuth } from '../../hooks/useAuth'
import { useBranding } from '../../hooks/useBranding'
import { useCancelCompanyAppointment } from '../../hooks/useCancelCompanyAppointment'
import { usePublicCompany } from '../../hooks/usePublicCompany'
import { formatTimeInZone } from '../../lib/companyTime'
import { formatInCompanyZone } from '../../lib/formatInCompanyZone'
import {
  CARD_TITLE_CLASSES,
  ERROR_CLASSES,
  LINK_CLASSES,
  MUTED_TEXT_CLASSES,
  PRIMARY_BUTTON_CLASSES,
  ROW_DANGER_BUTTON_CLASSES,
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
  onCancel: (appointment: AdminAppointment) => void
}

function AppointmentBlock({
  title,
  emptyText,
  appointments,
  timezone,
  showDate,
  onCancel,
}: AppointmentBlockProps) {
  return (
    <section className="space-y-3">
      <h3 className={CARD_TITLE_CLASSES}>
        {/* Con 0 queda solo el título: el estado vacío ya lo dice. */}
        {appointments.length > 0 ? `${title} (${appointments.length})` : title}
      </h3>

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
                <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                  Acciones
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
                  <td className={`${TABLE_CELL_CLASSES} whitespace-nowrap`}>
                    {/* "Hoy" puede traer completados, porque el job cierra los que ya terminaron: sin botón. */}
                    {appointment.status === 'PENDING' && (
                      <button
                        type="button"
                        onClick={() => onCancel(appointment)}
                        className={ROW_DANGER_BUTTON_CLASSES}
                      >
                        Cancelar
                      </button>
                    )}
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

  // El nombre de la empresa sale de la página pública. Sin él, el aviso de cancelación no trae el link de WhatsApp.
  const { company } = usePublicCompany(branding?.slug)
  const cancel = useCancelCompanyAppointment()

  const error = brandingError ?? summaryError
  const isLoading = isLoadingBranding || isLoadingSummary

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className={MUTED_TEXT_CLASSES}>Hola, {session?.user.firstName}</p>
        <Link to="/company/appointments/new" className={PRIMARY_BUTTON_CLASSES}>
          Nuevo turno
        </Link>
      </div>

      {cancel.notice && (
        <DismissibleNotice
          key={cancel.notice.id}
          kind={cancel.notice.kind}
          onDismiss={cancel.dismissNotice}
        >
          <CancelNoticeContent
            notice={cancel.notice}
            companyName={company?.name}
            timezone={branding?.timezone}
          />
        </DismissibleNotice>
      )}

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
            onCancel={cancel.start}
          />
          <AppointmentBlock
            title="Próximos turnos"
            emptyText="No hay próximos turnos"
            appointments={summary.upcoming}
            timezone={branding.timezone}
            showDate
            onCancel={cancel.start}
          />
          <Link to="/company/appointments" className={LINK_CLASSES}>
            Ver todos los turnos
          </Link>

          {cancel.target && (
            <AdminCancelAppointmentModal
              serviceName={cancel.target.serviceName}
              clientName={cancel.target.client.name}
              clientPhone={cancel.target.client.phone}
              startDateTime={cancel.target.startDateTime}
              timezone={branding.timezone}
              error={cancel.error}
              isLoading={cancel.isLoading}
              onConfirm={cancel.confirm}
              onClose={cancel.close}
            />
          )}
        </>
      )}
    </section>
  )
}
