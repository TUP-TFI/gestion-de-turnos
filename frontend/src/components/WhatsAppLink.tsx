import { buildWhatsAppUrl } from '../lib/whatsappLink'
import { MUTED_TEXT_CLASSES, ROW_BUTTON_CLASSES } from '../lib/formStyles'

// Solo para lectores de pantalla: el link abre otra pestaña.
const NEW_TAB_TEXT = ' (se abre en otra pestaña)'

interface WhatsAppLinkProps {
  phone: string | null | undefined
  text: string
  label?: string
  // Nombre accesible del link, cuando el texto visible solo no alcanza (por ejemplo, en una tabla con muchos botones).
  ariaLabel?: string
  // Lo que se muestra si el teléfono no sirve.
  missingPhoneText?: string
  // Aspecto del botón. Por defecto, el compacto de las filas de tabla.
  className?: string
}

// Botón que abre WhatsApp con el mensaje armado (Click to Chat). El sistema no registra si el mensaje salió. Es un
// link y no un window.open: se puede abrir con el botón del medio y los lectores de pantalla lo anuncian como link.
// Sin un teléfono que sirva, en lugar del botón dice por qué.
export function WhatsAppLink({
  phone,
  text,
  label = 'Avisar por WhatsApp',
  ariaLabel,
  missingPhoneText = 'Sin teléfono para WhatsApp',
  className = ROW_BUTTON_CLASSES,
}: WhatsAppLinkProps) {
  const url = buildWhatsAppUrl(phone, text)

  if (!url) {
    return (
      <span className={`${MUTED_TEXT_CLASSES} whitespace-nowrap`}>
        {missingPhoneText}
      </span>
    )
  }

  return (
    <a
      href={url}
      target="_blank"
      rel="noopener noreferrer"
      aria-label={ariaLabel ? `${ariaLabel}${NEW_TAB_TEXT}` : undefined}
      className={className}
    >
      {label}
      <span className="sr-only">{NEW_TAB_TEXT}</span>
    </a>
  )
}
