import { buildWhatsAppUrl } from '../lib/whatsappLink'
import { MUTED_TEXT_CLASSES, ROW_BUTTON_CLASSES } from '../lib/formStyles'

interface WhatsAppLinkProps {
  phone: string | null | undefined
  text: string
  label?: string
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
  className = ROW_BUTTON_CLASSES,
}: WhatsAppLinkProps) {
  const url = buildWhatsAppUrl(phone, text)

  if (!url) {
    return <span className={MUTED_TEXT_CLASSES}>Sin teléfono para WhatsApp</span>
  }

  return (
    <a href={url} target="_blank" rel="noopener noreferrer" className={className}>
      {label}
    </a>
  )
}
