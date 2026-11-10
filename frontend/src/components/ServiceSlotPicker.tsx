import { useRef, useState } from 'react'
import type { QueryKey } from '@tanstack/react-query'
import { ApiRequestError } from '../api/client'
import {
  BADGE_CLASSES,
  BADGE_VARIANT_CLASSES,
  CARD_TITLE_CLASSES,
  COMPANY_BUTTON_CLASSES,
  NOTICE_CLASSES,
  PAGE_TITLE_CLASSES,
  SERVICE_CARD_CLASSES,
  SERVICE_CARD_SELECTED_CLASSES,
} from '../lib/formStyles'
import type {
  Availability,
  PublicService,
  SlotSelection,
} from '../types/PublicCompany'
import { SlotPicker } from './SlotPicker'

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
  const [serviceId, setServiceId] = useState<string | null>(null)
  // Cada vez que se elige un servicio, también el que ya estaba elegido, el horario elegido se suelta.
  const [choice, setChoice] = useState(0)
  const [serviceGone, setServiceGone] = useState(false)
  const dateSectionRef = useRef<HTMLElement>(null)
  const dateInputRef = useRef<HTMLInputElement>(null)

  // Si el servicio dejó de estar en la lista, ya no cuenta como elegido.
  const service = services.find((item) => item.id === serviceId) ?? null

  function handleAvailabilityError(error: Error) {
    onAvailabilityError?.(error)
    if (error instanceof ApiRequestError && error.code === 'SERVICE_NOT_FOUND') {
      setServiceId(null)
      setChoice((current) => current + 1)
      setServiceGone(true)
    }
  }

  function chooseService(id: string) {
    setServiceId(id)
    setChoice((current) => current + 1)
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

        <SlotPicker
          timezone={timezone}
          enabled={service !== null}
          disabledMessage="Elegí un servicio para ver sus horarios."
          scope={`${service?.id ?? ''}:${choice}`}
          queryKey={(day) => queryKey(service?.id ?? '', day)}
          fetchAvailability={(day) => fetchAvailability(service?.id ?? '', day)}
          summaryLabel={service?.name ?? ''}
          actionLabel="Reservar"
          onSelect={(startDateTime) => {
            if (service) {
              onSelect({ service, startDateTime })
            }
          }}
          onAvailabilityError={handleAvailabilityError}
          dateInputRef={dateInputRef}
        />
      </section>
    </div>
  )
}
