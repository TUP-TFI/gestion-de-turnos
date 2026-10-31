import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Link, useLocation, useNavigate, useParams } from 'react-router'
import { ApiRequestError } from '../../api/client'
import { getAvailability } from '../../api/publicCompany'
import { CompanyNotFoundCard } from '../../components/CompanyNotFoundCard'
import { ServiceSlotPicker } from '../../components/ServiceSlotPicker'
import type { SlotSelection } from '../../components/ServiceSlotPicker'
import { useAuth } from '../../hooks/useAuth'
import { useClientAuth } from '../../hooks/useClientAuth'
import { usePublicCompany } from '../../hooks/usePublicCompany'
import { COMPANY_CATEGORY_LABELS } from '../../lib/companyCategories'
import { companyThemeStyle } from '../../lib/companyTheme'
import {
  COMPANY_BAND_BUTTON_CLASSES,
  COMPANY_BAND_CLASSES,
  ERROR_CLASSES,
  FIELDSET_CLASSES,
  LINK_CLASSES,
  MUTED_TEXT_CLASSES,
  NOTICE_CLASSES,
} from '../../lib/formStyles'
import { getDashboardPath } from '../../lib/panelRoutes'
import type { PublicCompany } from '../../types/PublicCompany'

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
      className="h-20 w-20 shrink-0 rounded-xl bg-white object-contain sm:h-24 sm:w-24"
    />
  )
}

function CompanyContact({ company }: { company: PublicCompany }) {
  return (
    <>
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
    </>
  )
}

function UnavailableNotice({ company }: { company: PublicCompany }) {
  return (
    <div className={FIELDSET_CLASSES}>
      <p className="text-body">
        {company.name} no está disponible por este medio.
      </p>
      <CompanyContact company={company} />
    </div>
  )
}

// Página pública de una empresa (US-06.1 y US-06.2), sin sesión. Es la misma para todas: solo cambia lo que trae el
// slug.
export function PublicCompanyPage() {
  const { slug = '' } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const queryClient = useQueryClient()
  const { session: panelSession } = useAuth()
  const { session, logout } = useClientAuth(slug)
  const { company, services, notFound, error, isLoading } =
    usePublicCompany(slug)
  const [bookingNotice, setBookingNotice] = useState<string | null>(null)

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

  // Provisorio, hasta que exista la confirmación de la reserva: Brune lo reemplaza por useBookingFlow.
  function handleSelect({ service, startDateTime }: SlotSelection) {
    if (!session) {
      const params = new URLSearchParams({
        serviceId: service.id,
        start: startDateTime,
      })
      navigate(`/${slug}/login?${params}`)
      return
    }
    setBookingNotice('La confirmación de la reserva todavía no está disponible.')
  }

  // La empresa se desactivó o el servicio dejó de estar: se vuelve a pedir la página para mostrar lo que hay ahora.
  function handleAvailabilityError(availabilityError: Error) {
    if (
      availabilityError instanceof ApiRequestError &&
      (availabilityError.code === 'COMPANY_INACTIVE' ||
        availabilityError.code === 'SERVICE_NOT_FOUND')
    ) {
      queryClient.invalidateQueries({ queryKey: ['public-company', slug] })
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
        <div className="mx-auto max-w-5xl space-y-8 px-4 py-6 sm:py-8">
          <div className="flex flex-wrap items-center justify-between gap-3">
            {canGoBack ? (
              <button
                type="button"
                onClick={handleBack}
                className={COMPANY_BAND_BUTTON_CLASSES}
              >
                ← Volver
              </button>
            ) : (
              <span />
            )}

            {/* En una empresa desactivada no se puede iniciar sesión, pero sí cerrar una que quedó abierta. */}
            {session ? (
              <div className="flex flex-wrap items-center gap-3">
                <span className="text-sm font-medium">
                  {session.user.firstName} {session.user.lastName}
                </span>
                <Link
                  to={`/${slug}/my-appointments`}
                  className={COMPANY_BAND_BUTTON_CLASSES}
                >
                  Mis turnos
                </Link>
                <button
                  type="button"
                  onClick={handleLogout}
                  className={COMPANY_BAND_BUTTON_CLASSES}
                >
                  Cerrar sesión
                </button>
              </div>
            ) : (
              isActive && (
                <Link
                  to={`/${slug}/login`}
                  className={COMPANY_BAND_BUTTON_CLASSES}
                >
                  Iniciar sesión
                </Link>
              )
            )}
          </div>

          <div className="flex items-center gap-4 sm:gap-6">
            {company.logoUrl && (
              <CompanyLogo key={company.logoUrl} url={company.logoUrl} />
            )}
            <div className="min-w-0 space-y-1">
              {/* Sin text-ink: el color del texto sale de la banda, según el contraste con el de la empresa. */}
              <h1 className="text-3xl font-semibold break-words sm:text-4xl">
                {company.name}
              </h1>
              <p className="text-sm font-medium">
                {COMPANY_CATEGORY_LABELS[company.category]}
              </p>
            </div>
          </div>

          {company.description && (
            <p className="max-w-prose">{company.description}</p>
          )}
        </div>
      </header>

      <main className="mx-auto max-w-5xl space-y-8 px-4 py-8">
        {isActive ? (
          <>
            {(company.address || company.phone || company.contactEmail) && (
              <div className="flex flex-wrap gap-x-8 gap-y-1">
                {company.address && (
                  <p className={MUTED_TEXT_CLASSES}>{company.address}</p>
                )}
                <CompanyContact company={company} />
              </div>
            )}

            {services.length === 0 ? (
              <p className={`${MUTED_TEXT_CLASSES} text-center`}>
                Todavía no hay servicios cargados
              </p>
            ) : (
              <ServiceSlotPicker
                services={services}
                timezone={company.timezone}
                queryKey={(serviceId, date) => [
                  'availability',
                  slug,
                  serviceId,
                  date,
                ]}
                fetchAvailability={(serviceId, date) =>
                  getAvailability(slug, serviceId, date)
                }
                onSelect={handleSelect}
                onAvailabilityError={handleAvailabilityError}
              />
            )}

            {bookingNotice && (
              <p role="status" className={NOTICE_CLASSES}>
                {bookingNotice}
              </p>
            )}
          </>
        ) : (
          <UnavailableNotice company={company} />
        )}
      </main>
    </div>
  )
}
