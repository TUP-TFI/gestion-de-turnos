import { Link } from 'react-router'
import { useServiceStatus } from '../../hooks/useServiceStatus'
import { useServices } from '../../hooks/useServices'
import {
  BADGE_CLASSES,
  BADGE_VARIANT_CLASSES,
  ERROR_CLASSES,
  LINK_CLASSES,
  MUTED_TEXT_CLASSES,
  PAGE_TITLE_CLASSES,
  PRIMARY_BUTTON_CLASSES,
  TABLE_CELL_CLASSES,
  TABLE_HEAD_CLASSES,
  TABLE_WRAPPER_CLASSES,
} from '../../lib/formStyles'
import type { ServiceStatus } from '../../types/Service'

const STATUS_LABELS: Record<ServiceStatus, string> = {
  ACTIVE: 'Activo',
  INACTIVE: 'Inactivo',
}

const STATUS_CLASSES: Record<ServiceStatus, string> = {
  ACTIVE: BADGE_VARIANT_CLASSES.success,
  INACTIVE: BADGE_VARIANT_CLASSES.neutral,
}

const PRICE_FORMAT = new Intl.NumberFormat('es-AR', {
  style: 'currency',
  currency: 'ARS',
})

export function ServicesPage() {
  const { services, error, isLoading } = useServices()
  const {
    toggleStatus,
    error: statusError,
    isLoading: isChangingStatus,
  } = useServiceStatus()

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className={PAGE_TITLE_CLASSES}>Servicios</h2>
        <Link to="/company/services/new" className={PRIMARY_BUTTON_CLASSES}>
          Nuevo servicio
        </Link>
      </div>

      {isLoading && <p className={MUTED_TEXT_CLASSES}>Cargando...</p>}

      {error && (
        <p role="alert" className={ERROR_CLASSES}>
          {error}
        </p>
      )}

      {statusError && (
        <p role="alert" className={ERROR_CLASSES}>
          {statusError}
        </p>
      )}

      {!isLoading && !error && services.length === 0 && (
        <p className={`${MUTED_TEXT_CLASSES} text-center`}>Todavía no hay servicios</p>
      )}

      {services.length > 0 && (
        <div className={TABLE_WRAPPER_CLASSES}>
          <table className="w-full text-left text-sm text-body">
            <thead className={TABLE_HEAD_CLASSES}>
              <tr>
                <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                  Nombre
                </th>
                <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                  Precio
                </th>
                <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                  Duración
                </th>
                <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                  Estado
                </th>
                <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                  Acciones
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-hairline">
              {services.map((service) => (
                <tr key={service.id}>
                  <td className={`${TABLE_CELL_CLASSES} font-medium text-ink`}>{service.name}</td>
                  <td className={`${TABLE_CELL_CLASSES} whitespace-nowrap`}>
                    {PRICE_FORMAT.format(service.price)}
                  </td>
                  <td className={`${TABLE_CELL_CLASSES} whitespace-nowrap`}>
                    {service.durationMinutes} min
                  </td>
                  <td className={TABLE_CELL_CLASSES}>
                    <span
                      className={`${BADGE_CLASSES} ${STATUS_CLASSES[service.status]}`}
                    >
                      {STATUS_LABELS[service.status]}
                    </span>
                  </td>
                  <td className={`${TABLE_CELL_CLASSES} whitespace-nowrap`}>
                    <div className="flex gap-4">
                      <Link
                        to={`/company/services/${service.id}/edit`}
                        className={LINK_CLASSES}
                      >
                        Editar
                      </Link>
                      <button
                        type="button"
                        onClick={() => toggleStatus(service)}
                        disabled={isChangingStatus}
                        className={`${LINK_CLASSES} cursor-pointer disabled:cursor-not-allowed disabled:text-muted`}
                      >
                        {service.status === 'ACTIVE' ? 'Desactivar' : 'Reactivar'}
                      </button>
                    </div>
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
