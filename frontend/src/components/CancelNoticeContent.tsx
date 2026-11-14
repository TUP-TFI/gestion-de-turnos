import type { CancelNotice } from '../hooks/useCancelCompanyAppointment'
import { NoticeWhatsAppLink } from './NoticeWhatsAppLink'

interface CancelNoticeContentProps {
  notice: CancelNotice
  // Sin el nombre de la empresa o su zona, el mensaje no se puede armar y no se muestra el link.
  companyName: string | undefined
  timezone: string | undefined
}

// Contenido del aviso de la agenda y del dashboard después de cancelar: el texto y, si el turno canceló el negocio y
// ya se conoce la empresa, el link para avisarle al cliente por WhatsApp.
export function CancelNoticeContent({
  notice,
  companyName,
  timezone,
}: CancelNoticeContentProps) {
  return (
    <>
      {notice.text}
      <NoticeWhatsAppLink
        appointment={notice.appointment}
        companyName={companyName}
        timezone={timezone}
      />
    </>
  )
}
