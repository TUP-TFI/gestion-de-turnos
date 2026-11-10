import { useEffect, useRef, useState } from 'react'
import { formatInCompanyZone } from '../lib/formatInCompanyZone'
import {
  CARD_TITLE_CLASSES,
  DANGER_BUTTON_CLASSES,
  ERROR_CLASSES,
  LABEL_CLASSES,
  MUTED_TEXT_CLASSES,
  SECONDARY_BUTTON_CLASSES,
  TEXTAREA_CLASSES,
} from '../lib/formStyles'

// Igual que el límite del backend (CancelAppointmentRequest).
const MAX_REASON_LENGTH = 200

interface AdminCancelAppointmentModalProps {
  serviceName: string
  clientName: string
  clientPhone: string | null
  // Instante UTC, tal como lo devolvió el backend.
  startDateTime: string
  timezone: string
  error: string | null
  isLoading: boolean
  onConfirm: (reason: string) => void
  onClose: () => void
}

// Modal para que el admin confirme la cancelación de un turno (US-09.3), con un motivo opcional. Muestra al cliente,
// que el modal del cliente no necesita. El botón destructivo va en rojo (DESIGN.md §10).
export function AdminCancelAppointmentModal({
  serviceName,
  clientName,
  clientPhone,
  startDateTime,
  timezone,
  error,
  isLoading,
  onConfirm,
  onClose,
}: AdminCancelAppointmentModalProps) {
  const dialogRef = useRef<HTMLDialogElement>(null)
  const [reason, setReason] = useState('')

  useEffect(() => {
    // En desarrollo, StrictMode corre el efecto dos veces.
    if (dialogRef.current && !dialogRef.current.open) {
      dialogRef.current.showModal()
    }
  }, [])

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby="admin-cancel-appointment-title"
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
        <h2 id="admin-cancel-appointment-title" className={CARD_TITLE_CLASSES}>
          ¿Cancelar este turno?
        </h2>

        <dl className="space-y-2">
          <div>
            <dt className={MUTED_TEXT_CLASSES}>Servicio</dt>
            <dd className="font-medium text-ink">{serviceName}</dd>
          </div>
          <div>
            <dt className={MUTED_TEXT_CLASSES}>Cliente</dt>
            <dd className="font-medium text-ink">
              {clientName}
              {clientPhone && (
                <span className="block font-normal text-body">
                  {clientPhone}
                </span>
              )}
            </dd>
          </div>
          <div>
            <dt className={MUTED_TEXT_CLASSES}>Fecha y hora</dt>
            <dd className="font-medium text-ink">
              {formatInCompanyZone(startDateTime, timezone)}
            </dd>
          </div>
        </dl>

        <div>
          <label htmlFor="admin-cancel-reason" className={LABEL_CLASSES}>
            Motivo (opcional)
          </label>
          <textarea
            id="admin-cancel-reason"
            rows={3}
            maxLength={MAX_REASON_LENGTH}
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            disabled={isLoading}
            className={TEXTAREA_CLASSES}
          />
          <p className={`${MUTED_TEXT_CLASSES} text-right`}>
            {reason.length}/{MAX_REASON_LENGTH}
          </p>
        </div>

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
            onClick={() => onConfirm(reason)}
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
