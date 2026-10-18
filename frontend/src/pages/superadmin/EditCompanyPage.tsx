import type { FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { CompanyFields } from '../../components/CompanyFields'
import { useCompany } from '../../hooks/useCompany'
import { useUpdateCompany } from '../../hooks/useUpdateCompany'
import { readCompanyData } from '../../lib/companyForm'
import {
  ERROR_CLASSES,
  FIELDSET_CLASSES,
  PRIMARY_BUTTON_CLASSES,
} from '../../lib/formStyles'
import { handleInvalidField, resetFieldValidity } from '../../lib/formValidation'

// La key remonta la pantalla al cambiar de empresa: así no se arrastra el estado del guardado anterior.
export function EditCompanyPage() {
  const { id = '' } = useParams()
  return <EditCompanyForm key={id} id={id} />
}

const BACK_LINK_CLASSES = 'font-medium text-brand hover:text-brand-dark hover:underline'

function EditCompanyForm({ id }: { id: string }) {
  const { company, error: loadError, isLoading: isLoadingCompany, isNotFound } =
    useCompany(id)
  const { updateCompany, updatedCompany, error, isLoading, reset } =
    useUpdateCompany(id)

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    updateCompany(readCompanyData(new FormData(event.currentTarget)))
  }

  if (isLoadingCompany) {
    return <p className="text-sm text-slate-600">Cargando...</p>
  }

  if (isNotFound) {
    return (
      <section className="space-y-4">
        <p className="text-sm text-slate-700">La empresa no existe</p>
        <Link to="/superadmin/companies" className={BACK_LINK_CLASSES}>
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
    <form
      className="space-y-6"
      onSubmit={handleSubmit}
      onInvalidCapture={handleInvalidField}
      onInputCapture={(event) => {
        resetFieldValidity(event)
        // Al volver a editar se limpia el resultado del último guardado: el error o el aviso de éxito.
        if (error || updatedCompany) {
          reset()
        }
      }}
    >
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-xl font-semibold text-slate-900">Editar empresa</h2>
        <Link to="/superadmin/companies" className={BACK_LINK_CLASSES}>
          Volver al listado
        </Link>
      </div>

      <div className={FIELDSET_CLASSES}>
        <dl className="space-y-2 text-sm text-slate-700">
          <div>
            <dt className="font-medium">Slug</dt>
            <dd>{company.slug}</dd>
          </div>
          <div>
            <dt className="font-medium">URL pública</dt>
            <dd>/{company.slug}</dd>
          </div>
        </dl>
        <p className="text-xs text-slate-500">El slug no se puede cambiar</p>
      </div>

      <CompanyFields defaultValues={company} />

      {error && (
        <p role="alert" className={ERROR_CLASSES}>
          {error}
        </p>
      )}

      {updatedCompany && (
        <p
          role="status"
          className="rounded-lg bg-green-50 px-3 py-2 text-sm text-green-700"
        >
          Cambios guardados
        </p>
      )}

      <button
        type="submit"
        disabled={isLoading}
        className={PRIMARY_BUTTON_CLASSES}
      >
        {isLoading ? 'Guardando...' : 'Guardar cambios'}
      </button>
    </form>
  )
}
