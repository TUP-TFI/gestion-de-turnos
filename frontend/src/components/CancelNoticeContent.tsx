import { appointmentMessage } from '../lib/whatsappMessages'
import { LINK_CLASSES } from '../lib/formStyles'
import type { CancelNotice } from '../hooks/useCancelCompanyAppointment'
import { WhatsAppLink } from './WhatsAppLink'

// Subrayado siempre: dentro del párrafo verde, solo el color no alcanza para distinguir un link del texto.
const NOTICE_LINK_CLASSES = `${LINK_CLASSES} underline`

interface CancelNoticeContentProps {
  notice: CancelNotice
  // Sin el nombre de la empresa o su zona, el mensaje no se puede armar y no se muestra el link.
  companyName: string | undefined
  timezone: string | undefined
}

// Contenido del aviso de la agenda y del dashboard después de cancelar: el texto y, si el turno canceló el negocio y
// ya se conoce la empresa, el link para avisarle al cliente por WhatsApp. Va dentro del <p> de DismissibleNotice, por
// eso solo lleva elementos en línea.
export function CancelNoticeContent({
  notice,
  companyName,
  timezone,
}: CancelNoticeContentProps) {
  const message =
    notice.appointment && companyName && timezone
      ? appointmentMessage(notice.appointment, companyName, timezone)
      : null

  return (
    <>
      {notice.text}
      {message && notice.appointment && (
        <>
          {' '}
          <WhatsAppLink
            phone={notice.appointment.client.phone}
            text={message}
            className={NOTICE_LINK_CLASSES}
          />
        </>
      )}
    </>
  )
}
