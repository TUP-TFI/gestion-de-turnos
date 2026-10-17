import { useState } from 'react'
import type { FormEvent } from 'react'
import { useCreateCompany } from '../../hooks/useCreateCompany'
import { COMPANY_CATEGORY_LABELS } from '../../lib/companyCategories'
import { DEFAULT_TIMEZONE, TIMEZONES } from '../../lib/timezones'
import {
  ERROR_CLASSES,
  INPUT_CLASSES,
  LABEL_CLASSES,
  PRIMARY_BUTTON_CLASSES,
} from '../../lib/formStyles'
import { handleInvalidField, resetFieldValidity } from '../../lib/formValidation'
import type { CompanyCategory } from '../../types/Company'

const FIELDSET_CLASSES =
  'space-y-4 rounded-xl border border-slate-200 bg-white p-4 sm:p-6'

const CATEGORIES = Object.keys(COMPANY_CATEGORY_LABELS) as CompanyCategory[]

function toNullable(value: FormDataEntryValue | null): string | null {
  const text = typeof value === 'string' ? value.trim() : ''
  return text === '' ? null : text
}

function toRequired(value: FormDataEntryValue | null): string {
  return typeof value === 'string' ? value.trim() : ''
}

export function CreateCompanyPage() {
  const { createCompany, createdCompany, error, isLoading, reset } =
    useCreateCompany()
  // Cambiar la key remonta el form y lo deja vacío.
  const [formKey, setFormKey] = useState(0)

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const data = new FormData(event.currentTarget)

    createCompany({
      company: {
        name: toRequired(data.get('name')),
        category: toRequired(data.get('category')) as CompanyCategory,
        timezone: toRequired(data.get('timezone')),
        description: toNullable(data.get('description')),
        address: toNullable(data.get('address')),
        phone: toNullable(data.get('phone')),
        contactEmail: toNullable(data.get('contactEmail')),
        logoUrl: toNullable(data.get('logoUrl')),
      },
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
        <button
          type="button"
          onClick={() => {
            reset()
            setFormKey((key) => key + 1)
          }}
          className={`mt-6 ${PRIMARY_BUTTON_CLASSES}`}
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
      onInputCapture={resetFieldValidity}
    >
      <h2 className="text-xl font-semibold text-slate-900">Nueva empresa</h2>

      <fieldset className={FIELDSET_CLASSES}>
        <legend className="px-2 text-sm font-semibold text-slate-900">
          Datos de la empresa
        </legend>

        <div>
          <label htmlFor="name" className={LABEL_CLASSES}>
            Nombre *
          </label>
          <input id="name" name="name" type="text" required className={INPUT_CLASSES} />
        </div>

        <div>
          <label htmlFor="category" className={LABEL_CLASSES}>
            Rubro *
          </label>
          <select
            id="category"
            name="category"
            required
            defaultValue=""
            className={INPUT_CLASSES}
          >
            <option value="" disabled>
              Elegí un rubro
            </option>
            {CATEGORIES.map((category) => (
              <option key={category} value={category}>
                {COMPANY_CATEGORY_LABELS[category]}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label htmlFor="timezone" className={LABEL_CLASSES}>
            Zona horaria *
          </label>
          {/* Input con datalist en vez de select: con más de 400 zonas, escribir "Cordoba" o "Madrid" filtra la lista. */}
          <input
            id="timezone"
            name="timezone"
            type="text"
            list="timezone-options"
            required
            autoComplete="off"
            defaultValue={DEFAULT_TIMEZONE}
            onChange={(event) => {
              const value = event.target.value.trim()
              event.target.setCustomValidity(
                value && !TIMEZONES.includes(value)
                  ? 'Elegí una zona horaria de la lista.'
                  : '',
              )
            }}
            className={INPUT_CLASSES}
          />
          <datalist id="timezone-options">
            {TIMEZONES.map((timezone) => (
              <option key={timezone} value={timezone} />
            ))}
          </datalist>
          <p className="mt-1 text-xs text-slate-500">
            Escribí una ciudad o región para buscar, por ejemplo Cordoba.
          </p>
        </div>

        <div>
          <label htmlFor="description" className={LABEL_CLASSES}>
            Descripción
          </label>
          <textarea
            id="description"
            name="description"
            rows={3}
            className={INPUT_CLASSES}
          />
        </div>

        <div>
          <label htmlFor="address" className={LABEL_CLASSES}>
            Dirección
          </label>
          <input id="address" name="address" type="text" className={INPUT_CLASSES} />
        </div>

        <div>
          <label htmlFor="phone" className={LABEL_CLASSES}>
            Teléfono
          </label>
          <input id="phone" name="phone" type="tel" className={INPUT_CLASSES} />
        </div>

        <div>
          <label htmlFor="contactEmail" className={LABEL_CLASSES}>
            Email de contacto
          </label>
          <input
            id="contactEmail"
            name="contactEmail"
            type="email"
            className={INPUT_CLASSES}
          />
        </div>

        <div>
          <label htmlFor="logoUrl" className={LABEL_CLASSES}>
            URL del logo
          </label>
          <input id="logoUrl" name="logoUrl" type="url" className={INPUT_CLASSES} />
        </div>
      </fieldset>

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
