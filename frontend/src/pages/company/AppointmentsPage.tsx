import { useSearchParams } from 'react-router'
import { AdminCancelAppointmentModal } from '../../components/AdminCancelAppointmentModal'
import { CancelNoticeContent } from '../../components/CancelNoticeContent'
import { AppointmentStatusBadge } from '../../components/AppointmentStatusBadge'
import { DismissibleNotice } from '../../components/DismissibleNotice'
import { Pagination } from '../../components/Pagination'
import { WhatsAppLink } from '../../components/WhatsAppLink'
import { useBranding } from '../../hooks/useBranding'
import { useCancelCompanyAppointment } from '../../hooks/useCancelCompanyAppointment'
import { useCompanyAppointments } from '../../hooks/useCompanyAppointments'
import { usePublicCompany } from '../../hooks/usePublicCompany'
import type { AppointmentFilters } from '../../api/companyAppointments'
import {
  CANCELLED_BY_LABELS,
  ORIGIN_LABELS,
  STATUS_LABELS,
} from '../../lib/appointmentLabels'
import { formatInCompanyZone } from '../../lib/formatInCompanyZone'
import {
  APPOINTMENT_NOTICE_LABELS,
  appointmentNotice,
} from '../../lib/whatsappMessages'
import {
  ERROR_CLASSES,
  INPUT_CLASSES,
  LABEL_CLASSES,
  MUTED_TEXT_CLASSES,
  PAGE_TITLE_CLASSES,
  ROW_DANGER_BUTTON_CLASSES,
  SECONDARY_BUTTON_CLASSES,
  TABLE_CELL_CLASSES,
  TABLE_HEAD_CLASSES,
  TABLE_WRAPPER_CLASSES,
} from '../../lib/formStyles'
import type {
  AppointmentOrigin,
  AppointmentStatus,
} from '../../types/Appointment'

const PRICE_FORMAT = new Intl.NumberFormat('es-AR', {
  style: 'currency',
  currency: 'ARS',
})

// La agenda abre en Pendientes. 'ALL' es solo del front y quiere decir "sin filtro": nunca viaja al back.
type StatusView = AppointmentStatus | 'ALL'

const DEFAULT_STATUS: StatusView = 'PENDING'

// Los filtros viven en la URL: se pueden recargar y compartir, y "volver" respeta el filtro. Un valor que no es
// de la lista se ignora y queda el de por defecto. Object.hasOwn y no `in`, que también acepta 'toString'.
function readStatus(params: URLSearchParams): StatusView {
  const status = params.get('status')
  if (status === 'ALL') {
    return 'ALL'
  }
  return status && Object.hasOwn(STATUS_LABELS, status)
    ? (status as AppointmentStatus)
    : DEFAULT_STATUS
}

function readFilters(
  params: URLSearchParams,
  statusView: StatusView,
): AppointmentFilters {
  const origin = params.get('origin')
  const rescheduled = params.get('rescheduled')

  return {
    status: statusView === 'ALL' ? undefined : statusView,
    origin:
      origin && Object.hasOwn(ORIGIN_LABELS, origin)
        ? (origin as AppointmentOrigin)
        : undefined,
    rescheduled:
      rescheduled === 'true' ? true : rescheduled === 'false' ? false : undefined,
  }
}

function readPage(params: URLSearchParams): number {
  const page = Number(params.get('page'))
  return Number.isInteger(page) && page > 0 ? page : 0
}

// Agenda del admin: todos los turnos de la empresa, con filtros y paginado (US-09.2).
export function AppointmentsPage() {
  const [params, setParams] = useSearchParams()
  const statusView = readStatus(params)
  const filters = readFilters(params, statusView)
  const page = readPage(params)
  const {
    branding,
    error: brandingError,
    isLoading: isLoadingBranding,
  } = useBranding()
  const {
    appointments,
    error: appointmentsError,
    isLoading: isLoadingAppointments,
    isFetching,
  } = useCompanyAppointments(filters, page)
  // El nombre de la empresa sale de la página pública: sin él no hay mensajes de WhatsApp, pero la tabla funciona.
  const { company } = usePublicCompany(branding?.slug)
  const cancel = useCancelCompanyAppointment()

  // Corrige la URL si pide una página que ya no existe.
  const isOutOfRange = usePageInRange(appointments, page)

  // Cambiar un filtro vuelve a la primera página. Sin valor, el filtro sale de la URL.
  function changeFilter(name: string, value: string) {
    cancel.dismissNotice()
    const next = new URLSearchParams(params)
    // La URL de la vista por defecto no lleva el filtro.
    if (value && !(name === 'status' && value === DEFAULT_STATUS)) {
      next.set(name, value)
    } else {
      next.delete(name)
    }
    next.delete('page')
    setParams(next)
  }

  function changePage(newPage: number) {
    cancel.dismissNotice()
    const next = new URLSearchParams(params)
    if (newPage > 0) {
      next.set('page', String(newPage))
    } else {
      next.delete('page')
    }
    setParams(next)
  }

  const error = brandingError ?? appointmentsError
  const isLoading = isLoadingBranding || isLoadingAppointments
  // Distinta de la vista por defecto: Pendientes, sin origen ni reprogramado.
  const hasChangedView = Boolean(
    statusView !== DEFAULT_STATUS ||
      filters.origin ||
      filters.rescheduled !== undefined,
  )
  const rows = appointments?.content ?? []
  const isRefreshing = isFetching && !isLoadingAppointments
  const showResults = appointments !== null && !error && !isOutOfRange

  let emptyText = 'No hay turnos con estos filtros'
  if (!filters.origin && filters.rescheduled === undefined) {
    if (statusView === 'PENDING') {
      emptyText = 'No hay turnos pendientes'
    } else if (statusView === 'ALL') {
      emptyText = 'Todavía no hay turnos'
    }
  }

  return (
    <section className="space-y-6">
      <h2 className={PAGE_TITLE_CLASSES}>Turnos</h2>

      <div className="grid gap-4 sm:grid-cols-3">
        <div>
          <label htmlFor="status" className={LABEL_CLASSES}>
            Estado
          </label>
          <select
            id="status"
            value={statusView}
            onChange={(event) => changeFilter('status', event.target.value)}
            className={INPUT_CLASSES}
          >
            <option value="ALL">Todos</option>
            <option value="PENDING">{STATUS_LABELS.PENDING}</option>
            <option value="COMPLETED">{STATUS_LABELS.COMPLETED}</option>
            <option value="CANCELLED">{STATUS_LABELS.CANCELLED}</option>
          </select>
        </div>
        <div>
          <label htmlFor="origin" className={LABEL_CLASSES}>
            Origen
          </label>
          <select
            id="origin"
            value={filters.origin ?? ''}
            onChange={(event) => changeFilter('origin', event.target.value)}
            className={INPUT_CLASSES}
          >
            <option value="">Todos</option>
            <option value="ONLINE">{ORIGIN_LABELS.ONLINE}</option>
            <option value="MANUAL">{ORIGIN_LABELS.MANUAL}</option>
          </select>
        </div>
        <div>
          <label htmlFor="rescheduled" className={LABEL_CLASSES}>
            Reprogramado
          </label>
          <select
            id="rescheduled"
            value={
              filters.rescheduled === undefined
                ? ''
                : String(filters.rescheduled)
            }
            onChange={(event) => changeFilter('rescheduled', event.target.value)}
            className={INPUT_CLASSES}
          >
            <option value="">Todos</option>
            <option value="true">Sí</option>
            <option value="false">No</option>
          </select>
        </div>
      </div>

      {hasChangedView && (
        <div>
          <button
            type="button"
            onClick={() => {
              cancel.dismissNotice()
              setParams(new URLSearchParams())
            }}
            className={SECONDARY_BUTTON_CLASSES}
          >
            Limpiar filtros
          </button>
        </div>
      )}

      {/* Arriba de la tabla y no en la fila: al cancelar, la agenda abre en Pendientes y el turno ya no está. */}
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

      {(isLoading || isOutOfRange) && (
        <p className={MUTED_TEXT_CLASSES}>Cargando...</p>
      )}

      {error && (
        <p role="alert" className={ERROR_CLASSES}>
          {error}
        </p>
      )}

      {appointments && showResults && (
        <div className="flex flex-wrap items-center justify-between gap-2">
          <p className={MUTED_TEXT_CLASSES}>
            {appointments.totalElements === 1
              ? '1 turno'
              : `${appointments.totalElements} turnos`}
          </p>
          <p className={MUTED_TEXT_CLASSES} aria-live="polite">
            {isRefreshing ? 'Actualizando...' : ''}
          </p>
        </div>
      )}

      {!isLoading && !error && !isOutOfRange && rows.length === 0 && (
        <p className={`${MUTED_TEXT_CLASSES} text-center`}>{emptyText}</p>
      )}

      {/* Sin la zona de la empresa no se puede mostrar la hora: si falla, solo el error. */}
      {branding && rows.length > 0 && !isOutOfRange && (
        <div
          aria-busy={isRefreshing}
          className={`${TABLE_WRAPPER_CLASSES} transition-opacity ${isRefreshing ? 'opacity-60' : ''}`}
        >
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
                  Cliente
                </th>
                <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                  Origen
                </th>
                <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                  Estado
                </th>
                <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                  Precio
                </th>
                <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                  Acciones
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-hairline">
              {rows.map((appointment) => {
                const notice = company
                  ? appointmentNotice(
                      appointment,
                      company.name,
                      branding.timezone,
                    )
                  : null
                return (
                  <tr key={appointment.id}>
                    <td className={`${TABLE_CELL_CLASSES} whitespace-nowrap`}>
                      {formatInCompanyZone(
                        appointment.startDateTime,
                        branding.timezone,
                      )}
                    </td>
                    <td className={`${TABLE_CELL_CLASSES} font-medium text-ink`}>
                      {appointment.serviceName}
                    </td>
                    <td className={TABLE_CELL_CLASSES}>
                      <p>{appointment.client.name}</p>
                      {appointment.client.phone && (
                        <p className={MUTED_TEXT_CLASSES}>
                          {appointment.client.phone}
                        </p>
                      )}
                    </td>
                    <td className={TABLE_CELL_CLASSES}>
                      {ORIGIN_LABELS[appointment.origin]}
                    </td>
                    <td className={TABLE_CELL_CLASSES}>
                      <div className="space-y-1">
                        <AppointmentStatusBadge appointment={appointment} />
                        {appointment.rescheduleCount > 0 &&
                          appointment.previousStartDateTime && (
                            <p className={MUTED_TEXT_CLASSES}>
                              Antes:{' '}
                              {formatInCompanyZone(
                                appointment.previousStartDateTime,
                                branding.timezone,
                              )}
                            </p>
                          )}
                        {appointment.status === 'CANCELLED' && (
                          <>
                            {appointment.cancelledBy && (
                              <p className={MUTED_TEXT_CLASSES}>
                                Canceló: {CANCELLED_BY_LABELS[appointment.cancelledBy]}
                              </p>
                            )}
                            {appointment.cancellationReason && (
                              <p className={MUTED_TEXT_CLASSES}>
                                Motivo: {appointment.cancellationReason}
                              </p>
                            )}
                          </>
                        )}
                      </div>
                    </td>
                    <td className={`${TABLE_CELL_CLASSES} whitespace-nowrap`}>
                      {PRICE_FORMAT.format(appointment.priceSnapshot)}
                    </td>
                    <td className={`${TABLE_CELL_CLASSES} whitespace-nowrap`}>
                      <div className="flex gap-2">
                        {/* Sin aviso que dar (o sin nombre de empresa) no hay botón. */}
                        {notice && (
                          <WhatsAppLink
                            phone={appointment.client.phone}
                            text={notice.text}
                            label="WhatsApp"
                            ariaLabel={`Avisar a ${appointment.client.name} por WhatsApp: ${APPOINTMENT_NOTICE_LABELS[notice.kind]}`}
                            missingPhoneText="Sin teléfono"
                          />
                        )}
                        {/* Sin plazo: el admin cancela en cualquier momento. Solo los pendientes se cancelan. */}
                        {appointment.status === 'PENDING' && (
                          <button
                            type="button"
                            aria-label={`Cancelar el turno de ${appointment.client.name}`}
                            onClick={() => cancel.start(appointment)}
                            className={ROW_DANGER_BUTTON_CLASSES}
                          >
                            Cancelar
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      {branding && cancel.target && (
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

      {appointments && !isOutOfRange && (
        <Pagination
          page={appointments.page}
          totalPages={appointments.totalPages}
          onChange={changePage}
        />
      )}
    </section>
  )
}
