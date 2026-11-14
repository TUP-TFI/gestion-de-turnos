import { NOTICE_LINK_CLASSES } from '../lib/formStyles'
import { appointmentMessage } from '../lib/whatsappMessages'
import type { AdminAppointment } from '../types/Appointment'
import { WhatsAppLink } from './WhatsAppLink'

interface NoticeWhatsAppLinkProps {
  appointment: AdminAppointment | null
  // Sin el nombre de la empresa o su zona, el mensaje no se puede armar y no se muestra el link.
  companyName: string | undefined
  timezone: string | undefined
}

// Link "Avisar por WhatsApp" para el final de un aviso de la página (cancelar o cargar un turno). El mensaje sale del
// estado del turno que devolvió el backend (`appointmentMessage`): sin mensaje, como en un turno cancelado por el
// cliente, no muestra nada. Va dentro del <p> de DismissibleNotice, por eso es un elemento en línea, con su espacio
// delante.
export function NoticeWhatsAppLink({
  appointment,
  companyName,
  timezone,
}: NoticeWhatsAppLinkProps) {
  const message =
    appointment && companyName && timezone
      ? appointmentMessage(appointment, companyName, timezone)
      : null

  if (!appointment || !message) {
    return null
  }

  return (
    <>
      {' '}
      <WhatsAppLink
        phone={appointment.client.phone}
        text={message}
        className={NOTICE_LINK_CLASSES}
      />
    </>
  )
}
