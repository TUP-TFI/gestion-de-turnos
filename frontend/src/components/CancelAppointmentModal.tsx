import { useEffect, useRef } from 'react'
import { formatInCompanyZone } from '../lib/formatInCompanyZone'
import {
  CARD_TITLE_CLASSES,
  DANGER_BUTTON_CLASSES,
  ERROR_CLASSES,
  MUTED_TEXT_CLASSES,
  SECONDARY_BUTTON_CLASSES,
} from '../lib/formStyles'

interface CancelAppointmentModalProps {
  serviceName: string
  // Instante UTC, tal como lo devolvió el backend.
  startDateTime: string
  timezone: string
  error: string | null
  isLoading: boolean
  onConfirm: () => void
  onClose: () => void
}

// Modal para confirmar la cancelación de un turno (US-08.1), con el botón destructivo en rojo (DESIGN.md §10).
export function CancelAppointmentModal({
  serviceName,
  startDateTime,
  timezone,
  error,
  isLoading,
  onConfirm,
  onClose,
}: CancelAppointmentModalProps) {
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
      aria-labelledby="cancel-appointment-title"
      // Al apretar Esc, el navegador cerraría el modal solo. Se frena para que no se cierre mientras se cancela.
      onCancel={(event) => {
        event.preventDefault()
        if (!isLoading) {
          onClose()
        }
      }}
      className="m-auto w-[calc(100%-2rem)] max-w-sm rounded-xl border border-hairline bg-white p-6 shadow-sm backdrop:bg-black/50"
    >
      <div className="space-y-4">
        <h2 id="cancel-appointment-title" className={CARD_TITLE_CLASSES}>
          ¿Cancelar este turno?
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
        </dl>

        <p className={MUTED_TEXT_CLASSES}>
          El turno no se puede volver a activar.
        </p>

        {error && (
          <p role="alert" className={ERROR_CLASSES}>
            {error}
          </p>
        )}

        <div className="flex justify-end gap-3">
          <button
            type="button"
            onClick={onClose}
            disabled={isLoading}
            className={SECONDARY_BUTTON_CLASSES}
          >
            Volver
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={isLoading}
            className={DANGER_BUTTON_CLASSES}
          >
            {isLoading ? 'Cancelando...' : 'Cancelar turno'}
          </button>
        </div>
      </div>
    </dialog>
  )
}
