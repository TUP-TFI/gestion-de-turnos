import { useEffect, useRef } from 'react'
import { formatInCompanyZone } from '../lib/formatInCompanyZone'
import {
  CARD_TITLE_CLASSES,
  COMPANY_BUTTON_CLASSES,
  ERROR_CLASSES,
  MUTED_TEXT_CLASSES,
  SECONDARY_BUTTON_CLASSES,
} from '../lib/formStyles'

interface BookingConfirmModalProps {
  companyName: string
  serviceName: string
  // Instante UTC, tal como lo devolvió el backend.
  startDateTime: string
  timezone: string
  error: string | null
  isLoading: boolean
  onConfirm: () => void
  onCancel: () => void
}

// Modal para revisar el turno antes de reservarlo (US-07.2). Va dentro del div que aplica companyThemeStyle, porque
// de ahí toma el color del botón "Confirmar".
export function BookingConfirmModal({
  companyName,
  serviceName,
  startDateTime,
  timezone,
  error,
  isLoading,
  onConfirm,
  onCancel,
}: BookingConfirmModalProps) {
  const dialogRef = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    // En desarrollo, StrictMode corre el efecto dos veces.
    if (dialogRef.current && !dialogRef.current.open) {
      dialogRef.current.showModal()
    }
  }, [])

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby="booking-confirm-title"
      // Al apretar Esc, el navegador cerraría el modal solo. Se frena para que no se cierre mientras se guarda el turno.
      onCancel={(event) => {
        event.preventDefault()
        if (!isLoading) {
          onCancel()
        }
      }}
      className="m-auto w-[calc(100%-2rem)] max-w-sm rounded-xl border border-hairline bg-white p-6 shadow-sm backdrop:bg-black/50"
    >
      <div className="space-y-4">
        <h2 id="booking-confirm-title" className={CARD_TITLE_CLASSES}>
          Confirmá tu turno
        </h2>

        <dl className="space-y-2">
          <div>
            <dt className={MUTED_TEXT_CLASSES}>Servicio</dt>
            <dd className="font-medium text-ink">{serviceName}</dd>
          </div>
          <div>
            <dt className={MUTED_TEXT_CLASSES}>Fecha y hora</dt>
            <dd className="font-medium text-ink">
              {formatInCompanyZone(startDateTime, timezone)}
            </dd>
          </div>
          <div>
            <dt className={MUTED_TEXT_CLASSES}>Empresa</dt>
            <dd className="font-medium text-ink">{companyName}</dd>
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
            onClick={onCancel}
            disabled={isLoading}
            className={SECONDARY_BUTTON_CLASSES}
          >
            Cancelar
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={isLoading}
            className={COMPANY_BUTTON_CLASSES}
          >
            {isLoading ? 'Reservando...' : 'Confirmar'}
          </button>
        </div>
      </div>
    </dialog>
  )
}
