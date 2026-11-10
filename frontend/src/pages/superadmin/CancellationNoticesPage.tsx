import { Link, useParams } from 'react-router'
import { WhatsAppLink } from '../../components/WhatsAppLink'
import { useCancellationNotices } from '../../hooks/useCancellationNotices'
import { useCompany } from '../../hooks/useCompany'
import { formatInCompanyZone } from '../../lib/formatInCompanyZone'
import {
  ERROR_CLASSES,
  LINK_CLASSES,
  MUTED_TEXT_CLASSES,
  PAGE_TITLE_CLASSES,
  TABLE_CELL_CLASSES,
  TABLE_HEAD_CLASSES,
  TABLE_WRAPPER_CLASSES,
} from '../../lib/formStyles'
import { companyClosedMessage } from '../../lib/whatsappMessages'

// Turnos futuros que se cancelaron al desactivar una empresa, con un botón para avisarle a cada cliente por WhatsApp
// (US-04.4). El sistema no registra a quién se avisó.
export function CancellationNoticesPage() {
  const { id = '' } = useParams()
  const {
    company,
    error: companyError,
    isLoading: isLoadingCompany,
  } = useCompany(id)
  const {
    data,
    error: noticesError,
    isLoading: isLoadingNotices,
  } = useCancellationNotices(id)

  const error = noticesError ?? companyError
  const isLoading = isLoadingCompany || isLoadingNotices

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className={PAGE_TITLE_CLASSES}>
          {company ? `Avisos de ${company.name}` : 'Avisos'}
        </h2>
        <Link to="/superadmin/companies" className={LINK_CLASSES}>
          Volver a empresas
        </Link>
      </div>

      <p className={MUTED_TEXT_CLASSES}>
        Son los turnos futuros que se cancelaron al desactivar la empresa. El
        sistema no registra a quién se avisó.
      </p>

      {isLoading && <p className={MUTED_TEXT_CLASSES}>Cargando...</p>}

      {error && (
        <p role="alert" className={ERROR_CLASSES}>
          {error}
        </p>
      )}

      {!isLoading && !error && data && data.notices.length === 0 && (
        <p className={`${MUTED_TEXT_CLASSES} text-center`}>
          No hay turnos futuros para avisar
        </p>
      )}

      {/* El mensaje lleva el nombre de la empresa: sin él todavía no se arma. */}
      {company && data && data.notices.length > 0 && (
        <div className={TABLE_WRAPPER_CLASSES}>
          <table className="w-full text-left text-sm text-body">
            <thead className={TABLE_HEAD_CLASSES}>
              <tr>
                <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                  Fecha y hora
                </th>
                <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                  Cliente
                </th>
                <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                  Servicio
                </th>
                <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                  Aviso
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-hairline">
              {data.notices.map((notice) => (
                <tr key={notice.appointmentId}>
                  <td className={`${TABLE_CELL_CLASSES} whitespace-nowrap`}>
                    {formatInCompanyZone(notice.startDateTime, data.timezone)}
                  </td>
                  <td className={`${TABLE_CELL_CLASSES} font-medium text-ink`}>
                    {notice.clientName}
                  </td>
                  <td className={TABLE_CELL_CLASSES}>{notice.serviceName}</td>
                  <td className={`${TABLE_CELL_CLASSES} whitespace-nowrap`}>
                    <WhatsAppLink
                      phone={notice.phone}
                      text={companyClosedMessage({
                        clientName: notice.clientName,
                        serviceName: notice.serviceName,
                        companyName: company.name,
                        startDateTime: notice.startDateTime,
                        timezone: data.timezone,
                      })}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
