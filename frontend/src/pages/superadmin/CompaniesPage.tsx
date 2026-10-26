import { Link } from 'react-router'
import { useCompanies } from '../../hooks/useCompanies'
import { useCompanyStatus } from '../../hooks/useCompanyStatus'
import { COMPANY_CATEGORY_LABELS } from '../../lib/companyCategories'
import {
  BADGE_CLASSES,
  BADGE_VARIANT_CLASSES,
  ERROR_CLASSES,
  MUTED_TEXT_CLASSES,
  PAGE_TITLE_CLASSES,
  PRIMARY_BUTTON_CLASSES,
  ROW_BUTTON_CLASSES,
  ROW_DANGER_BUTTON_CLASSES,
  TABLE_CELL_CLASSES,
  TABLE_HEAD_CLASSES,
  TABLE_WRAPPER_CLASSES,
} from '../../lib/formStyles'
import type { CompanyStatus } from '../../types/Company'

const STATUS_LABELS: Record<CompanyStatus, string> = {
  ACTIVE: 'Activa',
  INACTIVE: 'Inactiva',
}

const STATUS_CLASSES: Record<CompanyStatus, string> = {
  ACTIVE: BADGE_VARIANT_CLASSES.success,
  INACTIVE: BADGE_VARIANT_CLASSES.neutral,
}

export function CompaniesPage() {
  const { companies, error, isLoading } = useCompanies()
  const {
    toggleStatus,
    error: statusError,
    isLoading: isChangingStatus,
  } = useCompanyStatus()

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className={PAGE_TITLE_CLASSES}>Empresas</h2>
        <Link
          to="/superadmin/companies/new"
          className={PRIMARY_BUTTON_CLASSES}
        >
          Nueva empresa
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

      {!isLoading && !error && companies.length === 0 && (
        <p className={`${MUTED_TEXT_CLASSES} text-center`}>Todavía no hay empresas</p>
      )}

      {companies.length > 0 && (
        <div className={TABLE_WRAPPER_CLASSES}>
          <table className="w-full text-left text-sm text-body">
            <thead className={TABLE_HEAD_CLASSES}>
              <tr>
                <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                  Nombre
                </th>
                <th scope="col" className={`${TABLE_CELL_CLASSES} font-semibold`}>
                  Rubro
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
              {companies.map((company) => (
                <tr key={company.id}>
                  <td className={`${TABLE_CELL_CLASSES} font-medium text-ink`}>{company.name}</td>
                  <td className={TABLE_CELL_CLASSES}>
                    {COMPANY_CATEGORY_LABELS[company.category]}
                  </td>
                  <td className={TABLE_CELL_CLASSES}>
                    <span
                      className={`${BADGE_CLASSES} ${STATUS_CLASSES[company.status]}`}
                    >
                      {STATUS_LABELS[company.status]}
                    </span>
                  </td>
                  <td className={`${TABLE_CELL_CLASSES} whitespace-nowrap`}>
                    <div className="flex gap-2">
                      <Link
                        to={`/superadmin/companies/${company.id}/edit`}
                        className={ROW_BUTTON_CLASSES}
                      >
                        Editar
                      </Link>
                      <a
                        href={`/${company.slug}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className={ROW_BUTTON_CLASSES}
                      >
                        Ver página pública
                      </a>
                      <button
                        type="button"
                        onClick={() => toggleStatus(company)}
                        disabled={isChangingStatus}
                        className={
                          company.status === 'ACTIVE'
                            ? ROW_DANGER_BUTTON_CLASSES
                            : ROW_BUTTON_CLASSES
                        }
                      >
                        {company.status === 'ACTIVE' ? 'Desactivar' : 'Reactivar'}
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
