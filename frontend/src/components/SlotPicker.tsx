import { useState } from 'react'
import type { Ref } from 'react'
import { useQuery } from '@tanstack/react-query'
import type { QueryKey } from '@tanstack/react-query'
import { isApiResponseError, toErrorMessage } from '../api/client'
import { formatInCompanyZone } from '../lib/formatInCompanyZone'
import { formatTimeInZone, todayInZone } from '../lib/companyTime'
import {
  COMPANY_BUTTON_CLASSES,
  ERROR_CLASSES,
  FIELDSET_CLASSES,
  INPUT_CLASSES,
  LABEL_CLASSES,
  MUTED_TEXT_CLASSES,
  SLOT_BUTTON_CLASSES,
  SLOT_BUTTON_SELECTED_CLASSES,
} from '../lib/formStyles'
import type { Availability } from '../types/PublicCompany'

interface SlotPickerProps {
  timezone: string
  // Sin esto no se piden horarios y se muestra disabledMessage: por ejemplo, mientras no se eligió un servicio.
  enabled: boolean
  disabledMessage: string
  // Cuando cambia, el horario elegido deja de contar. Sirve para soltarlo al cambiar de servicio.
  scope: string
  queryKey: (date: string) => QueryKey
  fetchAvailability: (date: string) => Promise<Availability>
  // Lo que se muestra junto al horario elegido, antes de la fecha y hora: el nombre del servicio.
  summaryLabel: string
  actionLabel: string
  onSelect: (startDateTime: string) => void
  // Para que quien lo usa reaccione a un error de los horarios, por ejemplo recargando sus datos.
  onAvailabilityError?: (error: Error) => void
  dateInputRef?: Ref<HTMLInputElement>
}

// Elige fecha y horario entre los que devuelve fetchAvailability (US-06.2). Lo comparten la reserva y la
// reprogramación; cada una pone su fuente de horarios y su query key. Usa las variables de companyThemeStyle: va
// dentro de un wrapper que las defina.
export function SlotPicker({
  timezone,
  enabled,
  disabledMessage,
  scope,
  queryKey,
  fetchAvailability,
  summaryLabel,
  actionLabel,
  onSelect,
  onAvailabilityError,
  dateInputRef,
}: SlotPickerProps) {
  const today = todayInZone(timezone)
  const [date, setDate] = useState(today)
  const [picked, setPicked] = useState<{ scope: string; start: string } | null>(
    null,
  )

  const availability = useQuery({
    queryKey: queryKey(date),
    // Se atrapa acá y no en un efecto para avisar una sola vez por pedido fallido, en el momento en que falla.
    queryFn: async () => {
      try {
        return await fetchAvailability(date)
      } catch (error) {
        onAvailabilityError?.(error as Error)
        throw error
      }
    },
    enabled: enabled && Boolean(date),
    retry: (failureCount, error) =>
      !isApiResponseError(error) && failureCount < 3,
  })

  function chooseDate(value: string) {
    setDate(value)
    setPicked(null)
  }

  const starts = availability.data?.starts ?? []
  // Una actualización en segundo plano puede sacar el horario elegido: si otro lo reservó, deja de contar.
  const selectedStart =
    picked && picked.scope === scope && starts.includes(picked.start)
      ? picked.start
      : null

  return (
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
          {!enabled && <p className={MUTED_TEXT_CLASSES}>{disabledMessage}</p>}
          {enabled && !date && (
            <p className={MUTED_TEXT_CLASSES}>
              Elegí una fecha para ver sus horarios.
            </p>
          )}
          {availability.isLoading && (
            <p className={MUTED_TEXT_CLASSES}>Cargando...</p>
          )}
          {enabled && availability.error && !availability.data && (
            <p role="alert" className={ERROR_CLASSES}>
              {toErrorMessage(availability.error)}
            </p>
          )}
          {enabled && availability.data && starts.length === 0 && (
            <p className={MUTED_TEXT_CLASSES}>
              No hay horarios disponibles para este día
            </p>
          )}
          {enabled && starts.length > 0 && (
            <ul className="grid grid-cols-3 gap-2 sm:grid-cols-4 lg:grid-cols-6">
              {starts.map((item) => (
                <li key={item}>
                  <button
                    type="button"
                    aria-pressed={item === selectedStart}
                    onClick={() => setPicked({ scope, start: item })}
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

      {enabled && selectedStart && (
        <div className="flex flex-wrap items-center justify-between gap-3 border-t border-hairline pt-4">
          <p className="text-sm text-body">
            {summaryLabel}, {formatInCompanyZone(selectedStart, timezone)}
          </p>
          <button
            type="button"
            onClick={() => onSelect(selectedStart)}
            className={COMPANY_BUTTON_CLASSES}
          >
            {actionLabel}
          </button>
        </div>
      )}
    </div>
  )
}
