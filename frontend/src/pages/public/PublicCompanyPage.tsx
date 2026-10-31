import { useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router'
import { CompanyNotFoundCard } from '../../components/CompanyNotFoundCard'
import { useAuth } from '../../hooks/useAuth'
import { useClientAuth } from '../../hooks/useClientAuth'
import { usePublicCompany } from '../../hooks/usePublicCompany'
import { COMPANY_CATEGORY_LABELS } from '../../lib/companyCategories'
import { companyThemeStyle } from '../../lib/companyTheme'
import {
  CARD_TITLE_CLASSES,
  COMPANY_BAND_CLASSES,
  ERROR_CLASSES,
  FIELDSET_CLASSES,
  LINK_CLASSES,
  MUTED_TEXT_CLASSES,
  PAGE_TITLE_CLASSES,
  ROW_BUTTON_CLASSES,
  SECONDARY_BUTTON_CLASSES,
} from '../../lib/formStyles'
import { getDashboardPath } from '../../lib/panelRoutes'
import type { PublicCompany, PublicService } from '../../types/PublicCompany'

const PRICE_FORMAT = new Intl.NumberFormat('es-AR', {
  style: 'currency',
  currency: 'ARS',
})

// Si la imagen no carga, se oculta en lugar de mostrar el ícono roto. La página le pone una key con la URL, para que
// un logo nuevo vuelva a intentar. El alt va vacío porque el nombre de la empresa está al lado.
function CompanyLogo({ url }: { url: string }) {
  const [failed, setFailed] = useState(false)

  if (failed) {
    return null
  }

  return (
    <img
      src={url}
      alt=""
      onError={() => setFailed(true)}
      className="h-14 w-14 shrink-0 rounded-lg bg-white object-contain"
    />
  )
}

function ServiceList({ services }: { services: PublicService[] }) {
  return (
    <ul className="grid gap-4 sm:grid-cols-2">
      {services.map((service) => (
        <li key={service.id} className={FIELDSET_CLASSES}>
          <h3 className={CARD_TITLE_CLASSES}>{service.name}</h3>
          {service.description && (
            <p className="text-sm text-body">{service.description}</p>
          )}
          <p className="flex flex-wrap justify-between gap-2 text-sm font-medium text-ink">
            <span>{PRICE_FORMAT.format(service.price)}</span>
            <span>{service.durationMinutes} min</span>
          </p>
        </li>
      ))}
    </ul>
  )
}

function UnavailableNotice({ company }: { company: PublicCompany }) {
  return (
    <div className={FIELDSET_CLASSES}>
      <p className="text-body">
        {company.name} no está disponible por este medio.
      </p>
      {company.phone && (
        <p className={MUTED_TEXT_CLASSES}>
          Teléfono:{' '}
          <a href={`tel:${company.phone}`} className={LINK_CLASSES}>
            {company.phone}
          </a>
        </p>
      )}
      {company.contactEmail && (
        <p className={MUTED_TEXT_CLASSES}>
          Email:{' '}
          <a href={`mailto:${company.contactEmail}`} className={LINK_CLASSES}>
            {company.contactEmail}
          </a>
        </p>
      )}
    </div>
  )
}

// Página pública de una empresa (US-06.1), sin sesión. Es la misma para todas: solo cambia lo que trae el slug.
export function PublicCompanyPage() {
  const { slug = '' } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const { session: panelSession } = useAuth()
  const { session, logout } = useClientAuth(slug)
  const { company, services, notFound, error, isLoading } =
    usePublicCompany(slug)

  // 'default' es la primera pantalla de la pestaña: no hay adónde volver dentro de la app. Pasa al entrar directo o
  // desde "Marca", que abre otra pestaña; ahí un admin vuelve a su panel y un visitante no ve el botón.
  const hasHistory = location.key !== 'default'
  const panelPath = panelSession
    ? getDashboardPath(panelSession.user.role)
    : null
  const canGoBack = hasHistory || panelPath !== null

  function handleBack() {
    if (hasHistory) {
      navigate(-1)
    } else if (panelPath) {
      navigate(panelPath)
    }
  }

  function handleLogout() {
    if (window.confirm('¿Cerrar sesión?')) {
      logout()
    }
  }

  if (notFound) {
    return <CompanyNotFoundCard />
  }

  // Sin empresa todavía. Si ya se cargó y falla una actualización en segundo plano, la página sigue a la vista.
  if (!company) {
    return (
      <main className="min-h-screen bg-surface px-4 py-8">
        <div className="mx-auto max-w-5xl">
          {isLoading ? (
            <p className={MUTED_TEXT_CLASSES}>Cargando...</p>
          ) : (
            <p role="alert" className={ERROR_CLASSES}>
              {error}
            </p>
          )}
        </div>
      </main>
    )
  }

  const isActive = company.status === 'ACTIVE'

  return (
    <div
      className="min-h-screen bg-surface"
      style={companyThemeStyle(company.primaryColor)}
    >
      <header className={COMPANY_BAND_CLASSES}>
        {canGoBack && (
          <div className="mx-auto max-w-5xl px-4 pt-4">
            <button
              type="button"
              onClick={handleBack}
              className={ROW_BUTTON_CLASSES}
            >
              ← Volver
            </button>
          </div>
        )}
        <div className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-4 px-4 py-6">
          <div className="flex items-center gap-4">
            {company.logoUrl && (
              <CompanyLogo key={company.logoUrl} url={company.logoUrl} />
            )}
            <div>
              {/* Sin text-ink: el color del texto sale de la banda, según el contraste con el de la empresa. */}
              <h1 className="text-[22px] font-semibold">{company.name}</h1>
              <p className="text-sm">
                {COMPANY_CATEGORY_LABELS[company.category]}
              </p>
            </div>
          </div>

          {/* En una empresa desactivada no se puede iniciar sesión, pero sí cerrar una que quedó abierta. */}
          {session ? (
            <div className="flex flex-wrap items-center gap-3">
              <span className="text-sm font-medium">
                {session.user.firstName} {session.user.lastName}
              </span>
              <Link
                to={`/${slug}/my-appointments`}
                className={SECONDARY_BUTTON_CLASSES}
              >
                Mis turnos
              </Link>
              <button
                type="button"
                onClick={handleLogout}
                className={SECONDARY_BUTTON_CLASSES}
              >
                Cerrar sesión
              </button>
            </div>
          ) : (
            isActive && (
              <Link to={`/${slug}/login`} className={SECONDARY_BUTTON_CLASSES}>
                Iniciar sesión
              </Link>
            )
          )}
        </div>
      </header>

      <main className="mx-auto max-w-5xl space-y-6 px-4 py-8">
        {isActive ? (
          <>
            {(company.description || company.address) && (
              <div className="space-y-1">
                {company.description && (
                  <p className="text-body">{company.description}</p>
                )}
                {company.address && (
                  <p className={MUTED_TEXT_CLASSES}>{company.address}</p>
                )}
              </div>
            )}

            <h2 className={PAGE_TITLE_CLASSES}>Servicios</h2>
            {services.length === 0 ? (
              <p className={`${MUTED_TEXT_CLASSES} text-center`}>
                Todavía no hay servicios cargados
              </p>
            ) : (
              <ServiceList services={services} />
            )}
          </>
        ) : (
          <UnavailableNotice company={company} />
        )}
      </main>
    </div>
  )
}
