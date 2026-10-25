import type { FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { CompanyFields } from '../../components/CompanyFields'
import { useCompany } from '../../hooks/useCompany'
import { useResendActivation } from '../../hooks/useResendActivation'
import { useRedirectAfterSuccess } from '../../hooks/useRedirectAfterSuccess'
import { useUpdateCompany } from '../../hooks/useUpdateCompany'
import { readCompanyData } from '../../lib/companyForm'
import {
  BADGE_CLASSES,
  BADGE_VARIANT_CLASSES,
  CARD_TITLE_CLASSES,
  ERROR_CLASSES,
  FIELDSET_CLASSES,
  LINK_CLASSES,
  MUTED_TEXT_CLASSES,
  PAGE_TITLE_CLASSES,
  PRIMARY_BUTTON_CLASSES,
  SECONDARY_BUTTON_CLASSES,
  SUCCESS_CLASSES,
} from '../../lib/formStyles'
import { handleInvalidField, resetFieldValidity } from '../../lib/formValidation'
import type { UserStatus } from '../../types/User'

const ADMIN_STATUS_LABELS: Partial<Record<UserStatus, string>> = {
  ACTIVE: 'Activo',
  PENDING_ACTIVATION: 'Pendiente',
}

const ADMIN_STATUS_CLASSES: Partial<Record<UserStatus, string>> = {
  ACTIVE: BADGE_VARIANT_CLASSES.success,
  PENDING_ACTIVATION: BADGE_VARIANT_CLASSES.warning,
}

// La key remonta la pantalla al cambiar de empresa: así no se arrastra el estado del guardado anterior.
export function EditCompanyPage() {
  const { id = '' } = useParams()
  return <EditCompanyForm key={id} id={id} />
}

function EditCompanyForm({ id }: { id: string }) {
  const { company, error: loadError, isLoading: isLoadingCompany, isNotFound } =
    useCompany(id)
  const { updateCompany, updatedCompany, error, isLoading, reset } =
    useUpdateCompany(id)
  const {
    resend,
    sent: resendSent,
    error: resendError,
    isLoading: isResending,
  } = useResendActivation(id)
  // Guardado: muestra el aviso y a los pocos segundos vuelve al listado.
  useRedirectAfterSuccess(Boolean(updatedCompany), '/superadmin/companies')

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    updateCompany(readCompanyData(new FormData(event.currentTarget)))
  }

  if (isLoadingCompany) {
    return <p className={MUTED_TEXT_CLASSES}>Cargando...</p>
  }

  if (isNotFound) {
    return (
      <section className="space-y-4">
        <p className="text-sm text-body">La empresa no existe</p>
        <Link to="/superadmin/companies" className={LINK_CLASSES}>
          Volver al listado
        </Link>
      </section>
    )
  }

  if (!company) {
    return (
      <p role="alert" className={ERROR_CLASSES}>
        {loadError}
      </p>
    )
  }

  return (
    <div className="space-y-6">
      <form
        className="space-y-6"
        onSubmit={handleSubmit}
        onInvalidCapture={handleInvalidField}
        onInputCapture={(event) => {
          resetFieldValidity(event)
          if (error) {
            reset()
          }
        }}
      >
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className={PAGE_TITLE_CLASSES}>Editar empresa</h2>
          <Link to="/superadmin/companies" className={LINK_CLASSES}>
            Volver al listado
          </Link>
        </div>

        <div className={FIELDSET_CLASSES}>
          <dl className="space-y-2 text-sm text-body">
            <div>
              <dt className="font-medium">Slug</dt>
              <dd>{company.slug}</dd>
            </div>
            <div>
              <dt className="font-medium">URL pública</dt>
              <dd>/{company.slug}</dd>
            </div>
          </dl>
          <p className={MUTED_TEXT_CLASSES}>El slug no se puede cambiar</p>
        </div>

        <CompanyFields defaultValues={company} />

        {error && (
          <p role="alert" className={ERROR_CLASSES}>
            {error}
          </p>
        )}

        {updatedCompany && (
          <p role="status" className={SUCCESS_CLASSES}>
            Cambios guardados
          </p>
        )}

        <button
          type="submit"
          disabled={isLoading || Boolean(updatedCompany)}
          className={`w-full ${PRIMARY_BUTTON_CLASSES}`}
        >
          {isLoading ? 'Guardando...' : 'Guardar cambios'}
        </button>
      </form>

      <div className={FIELDSET_CLASSES}>
        <h3 className={CARD_TITLE_CLASSES}>Administrador</h3>
        {company.admin ? (
          <>
            <div className="flex flex-wrap items-center gap-3 text-sm text-body">
              <span>{company.admin.email}</span>
              <span
                className={`${BADGE_CLASSES} ${ADMIN_STATUS_CLASSES[company.admin.status] ?? BADGE_VARIANT_CLASSES.neutral}`}
              >
                {ADMIN_STATUS_LABELS[company.admin.status] ?? company.admin.status}
              </span>
            </div>
            {company.admin.status === 'PENDING_ACTIVATION' && (
              <button
                type="button"
                onClick={() => resend()}
                disabled={isResending}
                className={SECONDARY_BUTTON_CLASSES}
              >
                {isResending ? 'Reenviando...' : 'Reenviar link de activación'}
              </button>
            )}
            {resendError && (
              <p role="alert" className={ERROR_CLASSES}>
                {resendError}
              </p>
            )}
            {resendSent && (
              <p role="status" className={SUCCESS_CLASSES}>
                Se reenvió el link de activación
              </p>
            )}
          </>
        ) : (
          <p className={MUTED_TEXT_CLASSES}>La empresa no tiene administrador</p>
        )}
      </div>
    </div>
  )
}
