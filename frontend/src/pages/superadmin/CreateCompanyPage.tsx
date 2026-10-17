import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router'
import { CompanyFields } from '../../components/CompanyFields'
import { useCreateCompany } from '../../hooks/useCreateCompany'
import { readCompanyData, toRequired } from '../../lib/companyForm'
import {
  ERROR_CLASSES,
  FIELDSET_CLASSES,
  INPUT_CLASSES,
  LABEL_CLASSES,
  PRIMARY_BUTTON_CLASSES,
  SECONDARY_BUTTON_CLASSES,
} from '../../lib/formStyles'
import { handleInvalidField, resetFieldValidity } from '../../lib/formValidation'

export function CreateCompanyPage() {
  const { createCompany, createdCompany, error, isLoading, reset } =
    useCreateCompany()
  // Cambiar la key remonta el form y lo deja vacío.
  const [formKey, setFormKey] = useState(0)

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const data = new FormData(event.currentTarget)

    createCompany({
      company: readCompanyData(data),
      admin: {
        firstName: toRequired(data.get('adminFirstName')),
        lastName: toRequired(data.get('adminLastName')),
        email: toRequired(data.get('adminEmail')),
        phone: toRequired(data.get('adminPhone')),
      },
    })
  }

  if (createdCompany) {
    return (
      <section className="rounded-xl border border-slate-200 bg-white p-6">
        <h2 className="text-xl font-semibold text-slate-900">Empresa creada</h2>
        <dl className="mt-4 space-y-2 text-sm text-slate-700">
          <div>
            <dt className="font-medium">Nombre</dt>
            <dd>{createdCompany.name}</dd>
          </div>
          <div>
            <dt className="font-medium">Slug</dt>
            <dd>{createdCompany.slug}</dd>
          </div>
          <div>
            <dt className="font-medium">URL pública</dt>
            <dd>/{createdCompany.slug}</dd>
          </div>
        </dl>
        <p className="mt-4 text-sm text-slate-600">
          Se envió el link de activación al admin
        </p>
        <Link to="/superadmin/companies" className={`mt-6 block ${PRIMARY_BUTTON_CLASSES}`}>
          Ver empresas
        </Link>
        <button
          type="button"
          onClick={() => {
            reset()
            setFormKey((key) => key + 1)
          }}
          className={`mt-3 ${SECONDARY_BUTTON_CLASSES}`}
        >
          Crear otra empresa
        </button>
      </section>
    )
  }

  return (
    <form
      key={formKey}
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
      <h2 className="text-xl font-semibold text-slate-900">Nueva empresa</h2>

      <CompanyFields />

      <fieldset className={FIELDSET_CLASSES}>
        <legend className="px-2 text-sm font-semibold text-slate-900">
          Datos del admin
        </legend>

        <div>
          <label htmlFor="adminFirstName" className={LABEL_CLASSES}>
            Nombre *
          </label>
          <input
            id="adminFirstName"
            name="adminFirstName"
            maxLength={80}
            type="text"
            required
            className={INPUT_CLASSES}
          />
        </div>

        <div>
          <label htmlFor="adminLastName" className={LABEL_CLASSES}>
            Apellido *
          </label>
          <input
            id="adminLastName"
            name="adminLastName"
            maxLength={80}
            type="text"
            required
            className={INPUT_CLASSES}
          />
        </div>

        <div>
          <label htmlFor="adminEmail" className={LABEL_CLASSES}>
            Email *
          </label>
          <input
            id="adminEmail"
            name="adminEmail"
            maxLength={150}
            type="email"
            required
            className={INPUT_CLASSES}
          />
        </div>

        <div>
          <label htmlFor="adminPhone" className={LABEL_CLASSES}>
            Teléfono *
          </label>
          <input
            id="adminPhone"
            name="adminPhone"
            maxLength={30}
            type="tel"
            required
            className={INPUT_CLASSES}
          />
        </div>
      </fieldset>

      {error && (
        <p
          role="alert"
          className={ERROR_CLASSES}
        >
          {error}
        </p>
      )}

      <button
        type="submit"
        disabled={isLoading}
        className={PRIMARY_BUTTON_CLASSES}
      >
        {isLoading ? 'Creando...' : 'Crear empresa'}
      </button>
    </form>
  )
}
