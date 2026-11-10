import { useEffect, useRef, useState } from 'react'
import { getRescheduleAvailability } from '../api/clientAppointments'
import { formatInCompanyZone } from '../lib/formatInCompanyZone'
import {
  CARD_TITLE_CLASSES,
  COMPANY_BUTTON_CLASSES,
  ERROR_CLASSES,
  MUTED_TEXT_CLASSES,
  NOTICE_CLASSES,
  SECONDARY_BUTTON_CLASSES,
} from '../lib/formStyles'
import type { ClientAppointment } from '../types/Appointment'
import { SlotPicker } from './SlotPicker'

interface RescheduleModalProps {
  slug: string
  appointment: ClientAppointment
  timezone: string
  // Error de la confirmación, para mostrar en el modal.
  error: string | null
  // El horario elegido ya no está disponible: se vuelve a elegir.
  isSlotRejected: boolean
  isLoading: boolean
  onConfirm: (startDateTime: string) => void
  onClose: () => void
  onResetError: () => void
  onAvailabilityError: (error: Error) => void
}

// Modal para reprogramar un turno (US-08.2): se elige fecha y horario, y se confirma viendo la fecha vieja y la nueva.
// Va dentro del div que aplica companyThemeStyle, porque de ahí toman el color los botones y el horario elegido.
export function RescheduleModal({
  slug,
  appointment,
  timezone,
  error,
  isSlotRejected,
  isLoading,
  onConfirm,
  onClose,
  onResetError,
  onAvailabilityError,
}: RescheduleModalProps) {
  const dialogRef = useRef<HTMLDialogElement>(null)
  const [chosen, setChosen] = useState<string | null>(null)

  useEffect(() => {
    // En desarrollo, StrictMode corre el efecto dos veces.
    if (dialogRef.current && !dialogRef.current.open) {
      dialogRef.current.showModal()
    }
  }, [])

  const isConfirming = chosen !== null && !isSlotRejected

  function chooseStart(start: string) {
    onResetError()
    setChosen(start)
  }

  function backToPicker() {
    onResetError()
    setChosen(null)
  }

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby="reschedule-title"
      // Al apretar Esc, el navegador cerraría el modal solo. Se frena para que no se cierre mientras se guarda.
      onCancel={(event) => {
        event.preventDefault()
        if (!isLoading) {
          onClose()
        }
      }}
      className="m-auto max-h-[calc(100dvh-2rem)] w-[calc(100%-2rem)] max-w-3xl overflow-y-auto rounded-xl border border-hairline bg-white p-6 shadow-sm backdrop:bg-black/50"
    >
      <div className="space-y-4">
        <div>
          <h2 id="reschedule-title" className={CARD_TITLE_CLASSES}>
            {isConfirming ? 'Confirmá el cambio' : 'Reprogramar turno'}
          </h2>
          <p className={MUTED_TEXT_CLASSES}>
            {appointment.serviceName}, ahora el{' '}
            {formatInCompanyZone(appointment.startDateTime, timezone)}
          </p>
        </div>

        {/* Oculto y no desmontado, para que al volver siga la fecha que se estaba mirando. */}
        <div hidden={isConfirming} className="space-y-4">
          {isSlotRejected && error && (
            <p role="status" className={NOTICE_CLASSES}>
              {error}
            </p>
          )}

          <SlotPicker
            timezone={timezone}
            enabled
            disabledMessage=""
            scope={appointment.id}
            queryKey={(date) => [
              'reschedule-availability',
              slug,
              appointment.id,
              date,
            ]}
            fetchAvailability={(date) =>
              getRescheduleAvailability(appointment.id, date)
            }
            summaryLabel={appointment.serviceName}
            actionLabel="Continuar"
            onSelect={chooseStart}
            onAvailabilityError={onAvailabilityError}
          />

          <div className="flex justify-end">
            <button
              type="button"
              onClick={onClose}
              className={SECONDARY_BUTTON_CLASSES}
            >
              Cancelar
            </button>
          </div>
        </div>

        {isConfirming && chosen && (
          <div className="space-y-4">
            <dl className="space-y-2">
              <div>
                <dt className={MUTED_TEXT_CLASSES}>Servicio</dt>
                <dd className="font-medium text-ink">
                  {appointment.serviceName}
                </dd>
              </div>
              <div>
                <dt className={MUTED_TEXT_CLASSES}>Fecha actual</dt>
                <dd className="font-medium text-ink">
                  {formatInCompanyZone(appointment.startDateTime, timezone)}
                </dd>
              </div>
              <div>
                <dt className={MUTED_TEXT_CLASSES}>Nueva fecha</dt>
                <dd className="font-medium text-ink">
                  {formatInCompanyZone(chosen, timezone)}
                </dd>
              </div>
            </dl>

            {error && (
              <p role="alert" className={ERROR_CLASSES}>
                {error}
              </p>
            )}

            <div className="flex justify-end gap-3">
              <button
                type="button"
                onClick={backToPicker}
                disabled={isLoading}
                className={SECONDARY_BUTTON_CLASSES}
              >
                Elegir otro horario
              </button>
              <button
                type="button"
                onClick={() => onConfirm(chosen)}
                disabled={isLoading}
                className={COMPANY_BUTTON_CLASSES}
              >
                {isLoading ? 'Reprogramando...' : 'Confirmar'}
              </button>
            </div>
          </div>
        )}
      </div>
    </dialog>
  )
}
