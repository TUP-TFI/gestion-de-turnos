import { useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import type { QueryKey } from '@tanstack/react-query'
import {
  ApiRequestError,
  isApiResponseError,
  toErrorMessage,
} from '../api/client'
import { formatInCompanyZone } from '../lib/formatInCompanyZone'
import { formatTimeInZone, todayInZone } from '../lib/companyTime'
import {
  BADGE_CLASSES,
  BADGE_VARIANT_CLASSES,
  CARD_TITLE_CLASSES,
  COMPANY_BUTTON_CLASSES,
  ERROR_CLASSES,
  FIELDSET_CLASSES,
  INPUT_CLASSES,
  LABEL_CLASSES,
  MUTED_TEXT_CLASSES,
  NOTICE_CLASSES,
  PAGE_TITLE_CLASSES,
  SERVICE_CARD_CLASSES,
  SERVICE_CARD_SELECTED_CLASSES,
  SLOT_BUTTON_CLASSES,
  SLOT_BUTTON_SELECTED_CLASSES,
} from '../lib/formStyles'
import type {
  Availability,
  PublicService,
  SlotSelection,
} from '../types/PublicCompany'

const PRICE_FORMAT = new Intl.NumberFormat('es-AR', {
  style: 'currency',
  currency: 'ARS',
})

interface ServiceSlotPickerProps {
  services: PublicService[]
  timezone: string
  queryKey: (serviceId: string, date: string) => QueryKey
  fetchAvailability: (serviceId: string, date: string) => Promise<Availability>
  onSelect: (selection: SlotSelection) => void
  // Para que quien lo usa reaccione a un error de los horarios, por ejemplo recargando sus datos.
  onAvailabilityError?: (error: Error) => void
}

// Elige servicio, fecha y horario (US-06.2). No conoce la página: la fuente de horarios llega por props, para
// reutilizarlo al reprogramar y en la carga manual. Usa las variables de companyThemeStyle: va dentro de un wrapper
// que las defina.
export function ServiceSlotPicker({
  services,
  timezone,
  queryKey,
  fetchAvailability,
  onSelect,
  onAvailabilityError,
}: ServiceSlotPickerProps) {
  const today = todayInZone(timezone)
  const [serviceId, setServiceId] = useState<string | null>(null)
  const [date, setDate] = useState(today)
  const [start, setStart] = useState<string | null>(null)
  const [serviceGone, setServiceGone] = useState(false)
  const dateSectionRef = useRef<HTMLElement>(null)
  const dateInputRef = useRef<HTMLInputElement>(null)

  // Si el servicio dejó de estar en la lista, ya no cuenta como elegido.
  const service = services.find((item) => item.id === serviceId) ?? null

  function handleAvailabilityError(error: Error) {
    onAvailabilityError?.(error)
    if (error instanceof ApiRequestError && error.code === 'SERVICE_NOT_FOUND') {
      setServiceId(null)
      setStart(null)
      setServiceGone(true)
    }
  }

  const availability = useQuery({
    queryKey: queryKey(service?.id ?? '', date),
    // Se atrapa acá y no en un efecto para avisar una sola vez por pedido fallido, en el momento en que falla.
    queryFn: async () => {
      try {
        return await fetchAvailability(service?.id ?? '', date)
      } catch (error) {
        handleAvailabilityError(error as Error)
        throw error
      }
    },
    enabled: Boolean(service && date),
    retry: (failureCount, error) =>
      !isApiResponseError(error) && failureCount < 3,
  })

  function chooseService(id: string) {
    setServiceId(id)
    setStart(null)
    setServiceGone(false)
    goToDateSection()
  }

  // Lleva al paso siguiente, también si el servicio ya estaba elegido. El foco queda en la fecha, sin abrir el
  // calendario.
  function goToDateSection() {
    const reducedMotion = window.matchMedia(
      '(prefers-reduced-motion: reduce)',
    ).matches
    dateSectionRef.current?.scrollIntoView({
      behavior: reducedMotion ? 'auto' : 'smooth',
      block: 'start',
    })
    dateInputRef.current?.focus({ preventScroll: true })
  }

  function chooseDate(value: string) {
    setDate(value)
    setStart(null)
  }

  const starts = availability.data?.starts ?? []
  // Una actualización en segundo plano puede sacar el horario elegido: si otro lo reservó, deja de contar.
  const selectedStart = start && starts.includes(start) ? start : null

  return (
    <div className="space-y-8">
      <section className="space-y-4">
        <h2 className={PAGE_TITLE_CLASSES}>Elegí un servicio</h2>

        {serviceGone && (
          <p role="status" className={NOTICE_CLASSES}>
            Ese servicio ya no está disponible. Elegí otro.
          </p>
        )}

        <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {services.map((item) => {
            const isSelected = item.id === service?.id

            return (
              <li key={item.id}>
                <button
                  type="button"
                  aria-pressed={isSelected}
                  onClick={() => chooseService(item.id)}
                  className={`${SERVICE_CARD_CLASSES} ${isSelected ? SERVICE_CARD_SELECTED_CLASSES : 'border-hairline'}`}
                >
                  <span className={CARD_TITLE_CLASSES}>{item.name}</span>
                  {item.description && (
                    <span className="text-sm text-body">
                      {item.description}
                    </span>
                  )}
                  <span className="mt-auto flex flex-wrap items-center justify-between gap-2">
                    <span className="text-xl font-semibold text-ink">
                      {PRICE_FORMAT.format(item.price)}
                    </span>
                    <span
                      className={`${BADGE_CLASSES} ${BADGE_VARIANT_CLASSES.neutral}`}
                    >
                      {item.durationMinutes} min
                    </span>
                  </span>
                  {/* Dentro del botón de la tarjeta: es un span para no anidar dos botones. */}
                  <span className={COMPANY_BUTTON_CLASSES}>
                    {isSelected ? '✓ Elegido' : 'Elegir'}
                  </span>
                </button>
              </li>
            )
          })}
        </ul>
      </section>

      <section ref={dateSectionRef} className="space-y-4">
        <h2 className={PAGE_TITLE_CLASSES}>
          {service
            ? `Elegí día y horario para ${service.name}`
            : 'Elegí día y horario'}
        </h2>

        <div className={FIELDSET_CLASSES}>
          <div className="grid gap-6 md:grid-cols-[14rem_1fr]">
            <div>
              <label htmlFor="slot-date" className={LABEL_CLASSES}>
                Fecha
              </label>
              <input
                ref={dateInputRef}
                id="slot-date"
                type="date"
                min={today}
                value={date}
                onChange={(event) => chooseDate(event.target.value)}
                className={INPUT_CLASSES}
              />
            </div>

            <div aria-live="polite">
              {!service && (
                <p className={MUTED_TEXT_CLASSES}>
                  Elegí un servicio para ver sus horarios.
                </p>
              )}
              {service && !date && (
                <p className={MUTED_TEXT_CLASSES}>
                  Elegí una fecha para ver sus horarios.
                </p>
              )}
              {availability.isLoading && (
                <p className={MUTED_TEXT_CLASSES}>Cargando...</p>
              )}
              {service && availability.error && !availability.data && (
                <p role="alert" className={ERROR_CLASSES}>
                  {toErrorMessage(availability.error)}
                </p>
              )}
              {service && availability.data && starts.length === 0 && (
                <p className={MUTED_TEXT_CLASSES}>
                  No hay horarios disponibles para este día
                </p>
              )}
              {service && starts.length > 0 && (
                <ul className="grid grid-cols-3 gap-2 sm:grid-cols-4 lg:grid-cols-6">
                  {starts.map((item) => (
                    <li key={item}>
                      <button
                        type="button"
                        aria-pressed={item === selectedStart}
                        onClick={() => setStart(item)}
                        className={`w-full ${item === selectedStart ? SLOT_BUTTON_SELECTED_CLASSES : SLOT_BUTTON_CLASSES}`}
                      >
                        {formatTimeInZone(item, timezone)}
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>

          {service && selectedStart && (
            <div className="flex flex-wrap items-center justify-between gap-3 border-t border-hairline pt-4">
              <p className="text-sm text-body">
                {service.name}, {formatInCompanyZone(selectedStart, timezone)}
              </p>
              <button
                type="button"
                onClick={() =>
                  onSelect({ service, startDateTime: selectedStart })
                }
                className={COMPANY_BUTTON_CLASSES}
              >
                Reservar
              </button>
            </div>
          )}
        </div>
      </section>
    </div>
  )
}
