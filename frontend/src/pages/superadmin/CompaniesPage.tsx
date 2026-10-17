import { Link } from 'react-router'
import { useCompanies } from '../../hooks/useCompanies'
import { COMPANY_CATEGORY_LABELS } from '../../lib/companyCategories'
import { ERROR_CLASSES } from '../../lib/formStyles'
import type { CompanyStatus } from '../../types/Company'

const STATUS_LABELS: Record<CompanyStatus, string> = {
  ACTIVE: 'Activa',
  INACTIVE: 'Inactiva',
}

const STATUS_CLASSES: Record<CompanyStatus, string> = {
  ACTIVE: 'bg-green-50 text-green-700',
  INACTIVE: 'bg-slate-100 text-slate-600',
}

const ACTION_LINK_CLASSES = 'font-medium text-brand hover:text-brand-dark hover:underline'

export function CompaniesPage() {
  const { companies, error, isLoading } = useCompanies()

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-xl font-semibold text-slate-900">Empresas</h2>
        <Link to="/superadmin/companies/new" className={ACTION_LINK_CLASSES}>
          Nueva empresa
        </Link>
      </div>

      {isLoading && <p className="text-sm text-slate-600">Cargando...</p>}

      {error && (
        <p role="alert" className={ERROR_CLASSES}>
          {error}
        </p>
      )}

      {!isLoading && !error && companies.length === 0 && (
        <p className="text-sm text-slate-600">Todavía no hay empresas</p>
      )}

      {companies.length > 0 && (
        <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
          <table className="w-full text-left text-sm text-slate-700">
            <thead className="border-b border-slate-200 bg-slate-50 text-slate-900">
              <tr>
                <th scope="col" className="px-4 py-3 font-semibold">
                  Nombre
                </th>
                <th scope="col" className="px-4 py-3 font-semibold">
                  Rubro
                </th>
                <th scope="col" className="px-4 py-3 font-semibold">
                  Estado
                </th>
                <th scope="col" className="px-4 py-3 font-semibold">
                  Acciones
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {companies.map((company) => (
                <tr key={company.id}>
                  <td className="px-4 py-3 font-medium text-slate-900">{company.name}</td>
                  <td className="px-4 py-3">
                    {COMPANY_CATEGORY_LABELS[company.category]}
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={`rounded-full px-2 py-1 text-xs font-medium ${STATUS_CLASSES[company.status]}`}
                    >
                      {STATUS_LABELS[company.status]}
                    </span>
                  </td>
                  <td className="whitespace-nowrap px-4 py-3">
                    <div className="flex gap-4">
                      <Link
                        to={`/superadmin/companies/${company.id}/edit`}
                        className={ACTION_LINK_CLASSES}
                      >
                        Editar
                      </Link>
                      <a
                        href={`/${company.slug}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className={ACTION_LINK_CLASSES}
                      >
                        Ver página pública
                      </a>
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
