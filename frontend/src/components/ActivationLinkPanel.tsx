import { useState } from 'react'
import { activationMessage } from '../lib/whatsappMessages'
import { buildWhatsAppUrl } from '../lib/whatsappLink'
import {
  CARD_TITLE_CLASSES,
  INPUT_CLASSES,
  MUTED_TEXT_CLASSES,
  SECONDARY_BUTTON_CLASSES,
  SUCCESS_CLASSES,
} from '../lib/formStyles'
import { WhatsAppLink } from './WhatsAppLink'

interface ActivationLinkPanelProps {
  activationLink: string
  adminPhone: string
  companyName: string
  // Solo en el alta. Al generar un link nuevo no se tiene.
  adminName?: string
  // Al generar un link nuevo: el anterior deja de servir.
  replacesPrevious?: boolean
}

type CopyState = 'idle' | 'copied' | 'failed'

// Link de activación del admin, para que el superadmin se lo mande por WhatsApp o lo copie (US-04.1 y US-04.8). Lo usan
// el alta y el link nuevo. El link lleva el token y vence a las 48 horas.
export function ActivationLinkPanel({
  activationLink,
  adminPhone,
  companyName,
  adminName,
  replacesPrevious = false,
}: ActivationLinkPanelProps) {
  const [copyState, setCopyState] = useState<CopyState>('idle')
  const message = activationMessage({ adminName, companyName, activationLink })
  const hasWhatsApp = buildWhatsAppUrl(adminPhone, message) !== null

  async function handleCopy() {
    try {
      await navigator.clipboard.writeText(activationLink)
      setCopyState('copied')
    } catch {
      // Sin permiso o sin contexto seguro: el link queda en un campo para copiarlo a mano.
      setCopyState('failed')
    }
  }

  return (
    <div className="space-y-3">
      <h3 className={CARD_TITLE_CLASSES}>Link de activación del admin</h3>

      <div className="flex flex-wrap items-center gap-3">
        <WhatsAppLink
          phone={adminPhone}
          text={message}
          label="Enviar por WhatsApp"
          className={SECONDARY_BUTTON_CLASSES}
        />
        <button
          type="button"
          onClick={handleCopy}
          className={SECONDARY_BUTTON_CLASSES}
        >
          Copiar link
        </button>
      </div>

      {copyState === 'copied' && (
        <p role="status" className={SUCCESS_CLASSES}>
          Link copiado
        </p>
      )}

      {(copyState === 'failed' || !hasWhatsApp) && (
        <div>
          {copyState === 'failed' && (
            <p role="alert" className={MUTED_TEXT_CLASSES}>
              No se pudo copiar. Seleccioná el link y copialo a mano.
            </p>
          )}
          <label htmlFor="activation-link" className="sr-only">
            Link de activación
          </label>
          <input
            id="activation-link"
            type="text"
            readOnly
            value={activationLink}
            onFocus={(event) => event.currentTarget.select()}
            className={INPUT_CLASSES}
          />
        </div>
      )}

      <p className={MUTED_TEXT_CLASSES}>
        El link vence en 48 horas.
        {replacesPrevious && ' El link anterior ya no sirve.'}
      </p>
    </div>
  )
}
