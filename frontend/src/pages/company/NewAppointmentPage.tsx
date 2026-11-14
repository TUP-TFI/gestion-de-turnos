import { useState } from 'react'
import { Link } from 'react-router'
import { useQueryClient } from '@tanstack/react-query'
import { ClientSearch } from '../../components/ClientSearch'
import { DismissibleNotice } from '../../components/DismissibleNotice'
import { NoticeWhatsAppLink } from '../../components/NoticeWhatsAppLink'
import { SlotPicker } from '../../components/SlotPicker'
import { getCompanyAvailability } from '../../api/companyAppointments'
import { ApiRequestError } from '../../api/client'
import { useBranding } from '../../hooks/useBranding'
import { useCreateManualAppointment } from '../../hooks/useCreateManualAppointment'
import { usePublicCompany } from '../../hooks/usePublicCompany'
import { useServices } from '../../hooks/useServices'
import { companyThemeStyle } from '../../lib/companyTheme'
import { formatInCompanyZone } from '../../lib/formatInCompanyZone'
import { looksInternational } from '../../lib/whatsappLink'
import {
  ERROR_CLASSES,
  FIELDSET_CLASSES,
  INPUT_CLASSES,
  LABEL_CLASSES,
  LINK_CLASSES,
  MUTED_TEXT_CLASSES,
  NOTICE_CLASSES,
  NOTICE_LINK_CLASSES,
  PAGE_TITLE_CLASSES,
  SECONDARY_BUTTON_CLASSES,
} from '../../lib/formStyles'
import type { AdminAppointment } from '../../types/Appointment'
import type { CompanyClient } from '../../types/Client'

const PRICE_FORMAT = new Intl.NumberFormat('es-AR', {
  style: 'currency',
  currency: 'ARS',
})

const NAME_MAX_LENGTH = 160
const PHONE_MAX_LENGTH = 30

type ClientMode = 'account' | 'manual'

// Cada aviso lleva un id, para que se muestre con `key`: uno nuevo es un componente nuevo y se enfoca solo al
// montarse (DismissibleNotice).
let nextNoticeId = 0

interface CreatedNotice {
  id: number
  appointment: AdminAppointment
}

interface NewAppointmentFormProps {
  timezone: string
  onCreated: (appointment: AdminAppointment) => void
}

// El formulario entero. Para reiniciarlo, la página le cambia la `key`, como en CreateCompanyPage.
function NewAppointmentForm({ timezone, onCreated }: NewAppointmentFormProps) {
  const queryClient = useQueryClient()
  const { services, error: servicesError, isLoading } = useServices()
  const {
    create,
    error,
    errorCode,
    isLoading: isCreating,
    reset,
  } = useCreateManualAppointment()

  const [mode, setMode] = useState<ClientMode>('account')
  const [client, setClient] = useState<CompanyClient | null>(null)
  const [manualName, setManualName] = useState('')
  const [manualPhone, setManualPhone] = useState('')
  // Se calcula al salir del campo, no en cada tecla. Es un aviso: no bloquea cargar el turno.
  const [phoneWarning, setPhoneWarning] = useState(false)
  const [serviceId, setServiceId] = useState('')
  // Falta algún dato del cliente, o el elegido ya no existe. Se muestra junto al cliente.
  const [clientError, setClientError] = useState<string | null>(null)
  // A dónde va el foco cuando cambia el bloque del cliente: a "Cambiar" tras elegir uno, y al buscador tras "Cambiar".
  // Con null no se mueve: al cargar la página, o después de reiniciar el formulario, el foco no es de este bloque.
  const [focusNext, setFocusNext] = useState<'change' | 'search' | null>(null)

  const activeServices = services.filter(
    (service) => service.status === 'ACTIVE',
  )
  // Si el servicio elegido dejó de estar en la lista, ya no cuenta como elegido.
  const service = activeServices.find((item) => item.id === serviceId) ?? null

  // El cliente que se va a cargar, para el resumen antes de confirmar. Vacío mientras no se eligió o escribió.
  const clientLabel =
    mode === 'account'
      ? client
        ? `${client.lastName}, ${client.firstName}`
        : ''
      : manualName.trim()

  function chooseMode(newMode: ClientMode) {
    setMode(newMode)
    setClientError(null)
    setFocusNext(null)
    setPhoneWarning(false)
    // El error del turno anterior era de otra identidad.
    reset()
  }

  function chooseClient(chosen: CompanyClient) {
    setClient(chosen)
    setClientError(null)
    setFocusNext('change')
  }

  function changeClient() {
    setClient(null)
    setFocusNext('search')
  }

  // El cliente tiene que estar completo antes de mandar nada. Devuelve el cuerpo de la identidad, o null.
  function readIdentity() {
    if (mode === 'account') {
      if (!client) {
        setClientError('Elegí un cliente de la lista.')
        return null
      }
      return { clientId: client.id }
    }
    const name = manualName.trim()
    const phone = manualPhone.trim()
    if (!name || !phone) {
      setClientError('Completá el nombre y el teléfono del cliente.')
      return null
    }
    return { manualClientName: name, manualClientPhone: phone }
  }

  function handleSelectSlot(startDateTime: string) {
    // El botón del selector no se deshabilita: un segundo clic mientras se carga no manda nada.
    if (!service || isCreating) {
      return
    }
    const identity = readIdentity()
    if (!identity) {
      return
    }
    setClientError(null)
    create(
      { serviceId: service.id, startDateTime, ...identity },
      {
        onSuccess: onCreated,
        onError: (code) => {
          if (code === 'CLIENT_NOT_FOUND') {
            setClient(null)
            setFocusNext(null)
            setClientError(
              'El cliente elegido ya no está. Buscá otro de la lista.',
            )
          } else if (code === 'SERVICE_NOT_FOUND') {
            setServiceId('')
          }
        },
      },
    )
  }

  function handleAvailabilityError(availabilityError: Error) {
    if (
      availabilityError instanceof ApiRequestError &&
      availabilityError.code === 'SERVICE_NOT_FOUND'
    ) {
      setServiceId('')
      queryClient.invalidateQueries({ queryKey: ['services'] })
    }
  }

  if (isLoading) {
    return <p className={MUTED_TEXT_CLASSES}>Cargando...</p>
  }

  if (servicesError) {
    return (
      <p role="alert" className={ERROR_CLASSES}>
        {servicesError}
      </p>
    )
  }

  if (activeServices.length === 0) {
    return (
      <p className={`${MUTED_TEXT_CLASSES} text-center`}>
        Primero cargá un servicio.{' '}
        <Link to="/company/services/new" className={LINK_CLASSES}>
          Nuevo servicio
        </Link>
      </p>
    )
  }

  // El horario rechazado o el servicio que ya no está: un aviso, y el resto del formulario se mantiene. Cualquier
  // otro error, incluido INVALID_CLIENT_IDENTITY, va como error.
  const isNotice =
    errorCode === 'SLOT_UNAVAILABLE' ||
    errorCode === 'BOOKING_TOO_SOON' ||
    errorCode === 'BOOKING_TOO_FAR' ||
    errorCode === 'SERVICE_NOT_FOUND'

  return (
    <div className="space-y-6">
      <fieldset className={FIELDSET_CLASSES}>
        <legend className="px-2 text-sm font-semibold text-ink">
          1. Cliente
        </legend>

        <div className="flex flex-wrap gap-x-6 gap-y-2">
          <label className="flex items-center gap-2 text-sm text-ink">
            <input
              type="radio"
              name="client-mode"
              checked={mode === 'account'}
              onChange={() => chooseMode('account')}
            />
            Cliente con cuenta
          </label>
          <label className="flex items-center gap-2 text-sm text-ink">
            <input
              type="radio"
              name="client-mode"
              checked={mode === 'manual'}
              onChange={() => chooseMode('manual')}
            />
            Cliente sin cuenta
          </label>
        </div>

        {mode === 'account' &&
          (client ? (
            <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-hairline bg-surface-card px-3 py-2">
              <div className="text-sm">
                <p className="font-medium text-ink">
                  {client.lastName}, {client.firstName}
                </p>
                <p className={MUTED_TEXT_CLASSES}>
                  {client.email} · {client.phone}
                </p>
              </div>
              <button
                type="button"
                onClick={changeClient}
                autoFocus={focusNext === 'change'}
                className={SECONDARY_BUTTON_CLASSES}
              >
                Cambiar
              </button>
            </div>
          ) : (
            <ClientSearch
              onSelect={chooseClient}
              autoFocus={focusNext === 'search'}
            />
          ))}

        {mode === 'manual' && (
          <div className="space-y-4">
            <div>
              <label htmlFor="manual-client-name" className={LABEL_CLASSES}>
                Nombre *
              </label>
              <input
                id="manual-client-name"
                type="text"
                maxLength={NAME_MAX_LENGTH}
                value={manualName}
                onChange={(event) => {
                  setManualName(event.target.value)
                  setClientError(null)
                }}
                className={INPUT_CLASSES}
              />
            </div>
            <div>
              <label htmlFor="manual-client-phone" className={LABEL_CLASSES}>
                Teléfono *
              </label>
              <input
                id="manual-client-phone"
                type="tel"
                maxLength={PHONE_MAX_LENGTH}
                aria-describedby={`manual-client-phone-help${phoneWarning ? ' manual-client-phone-warning' : ''}`}
                value={manualPhone}
                onChange={(event) => {
                  setManualPhone(event.target.value)
                  setClientError(null)
                  setPhoneWarning(false)
                }}
                onBlur={() =>
                  setPhoneWarning(
                    manualPhone.trim() !== '' && !looksInternational(manualPhone),
                  )
                }
                className={INPUT_CLASSES}
              />
              <p id="manual-client-phone-help" className={MUTED_TEXT_CLASSES}>
                Con código de país, por ejemplo +54 9 351 ..., para poder
                avisarle por WhatsApp.
              </p>
              {phoneWarning && (
                <p
                  id="manual-client-phone-warning"
                  role="status"
                  className={`mt-2 ${NOTICE_CLASSES}`}
                >
                  Sin código de país, WhatsApp puede abrir otro chat.
                </p>
              )}
            </div>
          </div>
        )}

        {clientError && (
          <p role="alert" className={ERROR_CLASSES}>
            {clientError}
          </p>
        )}
      </fieldset>

      <fieldset className={FIELDSET_CLASSES}>
        <legend className="px-2 text-sm font-semibold text-ink">
          2. Servicio
        </legend>
        <div>
          <label htmlFor="appointment-service" className={LABEL_CLASSES}>
            Servicio *
          </label>
          <select
            id="appointment-service"
            value={service?.id ?? ''}
            onChange={(event) => {
              setServiceId(event.target.value)
              // Un error de horario o de servicio era del servicio anterior.
              reset()
            }}
            className={INPUT_CLASSES}
          >
            <option value="">Elegí un servicio</option>
            {activeServices.map((item) => (
              <option key={item.id} value={item.id}>
                {item.name} · {PRICE_FORMAT.format(item.price)} ·{' '}
                {item.durationMinutes} min
              </option>
            ))}
          </select>
        </div>
      </fieldset>

      <fieldset className={FIELDSET_CLASSES}>
        <legend className="px-2 text-sm font-semibold text-ink">
          3. Fecha y horario
        </legend>

        {/* CLIENT_NOT_FOUND ya se avisa en el bloque del cliente. */}
        {error && errorCode !== 'CLIENT_NOT_FOUND' && (
          <p
            role="alert"
            className={isNotice ? NOTICE_CLASSES : ERROR_CLASSES}
          >
            {error}
          </p>
        )}

        {/* El selector usa --company-accent: en el panel no hay wrapper que lo defina, así que va el neutro (ink). */}
        <div style={companyThemeStyle(null)}>
          <SlotPicker
            timezone={timezone}
            // El recuadro ya es el del paso.
            framed={false}
            enabled={Boolean(service)}
            disabledMessage="Elegí un servicio para ver sus horarios."
            scope={service?.id ?? ''}
            queryKey={(date) => ['company-availability', service?.id, date]}
            fetchAvailability={(date) =>
              getCompanyAvailability(service?.id ?? '', date)
            }
            summaryLabel={
              service
                ? clientLabel
                  ? `${service.name} · ${clientLabel}`
                  : service.name
                : ''
            }
            actionLabel={isCreating ? 'Cargando...' : 'Cargar turno'}
            onSelect={handleSelectSlot}
            onAvailabilityError={handleAvailabilityError}
          />
        </div>
      </fieldset>
    </div>
  )
}

// Carga manual de un turno que llegó por WhatsApp o por teléfono (US-09.4 a 09.6): cliente con o sin cuenta, servicio, y
// fecha y horario con la misma disponibilidad que la reserva online.
export function NewAppointmentPage() {
  const { branding, error: brandingError, isLoading } = useBranding()
  // El nombre de la empresa sale de la página pública: sin él no hay mensaje de WhatsApp, pero el turno se carga igual.
  const { company } = usePublicCompany(branding?.slug)
  const [notice, setNotice] = useState<CreatedNotice | null>(null)
  const [formKey, setFormKey] = useState(0)

  function handleCreated(appointment: AdminAppointment) {
    setNotice({ id: ++nextNoticeId, appointment })
    setFormKey((key) => key + 1)
  }

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className={PAGE_TITLE_CLASSES}>Nuevo turno</h2>
        <Link to="/company/appointments" className={SECONDARY_BUTTON_CLASSES}>
          Volver a la agenda
        </Link>
      </div>

      {notice && branding && (
        <DismissibleNotice
          key={notice.id}
          kind="success"
          onDismiss={() => setNotice(null)}
        >
          Cargaste el turno de {notice.appointment.client.name} (
          {notice.appointment.serviceName}) para el{' '}
          {formatInCompanyZone(notice.appointment.startDateTime, branding.timezone)}.{' '}
          <Link to="/company/appointments" className={NOTICE_LINK_CLASSES}>
            Ver en la agenda
          </Link>
          <NoticeWhatsAppLink
            appointment={notice.appointment}
            companyName={company?.name}
            timezone={branding.timezone}
          />
        </DismissibleNotice>
      )}

      {isLoading && <p className={MUTED_TEXT_CLASSES}>Cargando...</p>}

      {brandingError && (
        <p role="alert" className={ERROR_CLASSES}>
          {brandingError}
        </p>
      )}

      {/* Sin la zona de la empresa no se pueden mostrar horarios: si falla, solo el error. */}
      {branding && (
        <NewAppointmentForm
          key={formKey}
          timezone={branding.timezone}
          onCreated={handleCreated}
        />
      )}
    </section>
  )
}
