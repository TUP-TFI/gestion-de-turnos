import { COMPANY_CATEGORY_LABELS } from '../lib/companyCategories'
import { FIELDSET_CLASSES, INPUT_CLASSES, LABEL_CLASSES, MUTED_TEXT_CLASSES, TEXTAREA_CLASSES } from '../lib/formStyles'
import { DEFAULT_TIMEZONE, TIMEZONES } from '../lib/timezones'
import type { CompanyCategory, CompanyData } from '../types/Company'

const CATEGORIES = Object.keys(COMPANY_CATEGORY_LABELS) as CompanyCategory[]

interface CompanyFieldsProps {
  // Valores iniciales para la edición. Los inputs no son controlados: se leen con readCompanyData al enviar.
  defaultValues?: CompanyData
}

// Campos de la empresa que comparten el alta y la edición.
export function CompanyFields({ defaultValues }: CompanyFieldsProps) {
  return (
    <fieldset className={FIELDSET_CLASSES}>
      <legend className="px-2 text-sm font-semibold text-ink">
        Datos de la empresa
      </legend>

      <div>
        <label htmlFor="name" className={LABEL_CLASSES}>
          Nombre *
        </label>
        <input
          id="name"
          name="name"
          type="text"
          required
          maxLength={120}
          defaultValue={defaultValues?.name}
          className={INPUT_CLASSES}
        />
      </div>

      <div>
        <label htmlFor="category" className={LABEL_CLASSES}>
          Rubro *
        </label>
        <select
          id="category"
          name="category"
          required
          defaultValue={defaultValues?.category ?? ''}
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
          maxLength={50}
          autoComplete="off"
          defaultValue={defaultValues?.timezone ?? DEFAULT_TIMEZONE}
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
        <p className={`mt-1 ${MUTED_TEXT_CLASSES}`}>
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
          defaultValue={defaultValues?.description ?? ''}
          className={TEXTAREA_CLASSES}
        />
      </div>

      <div>
        <label htmlFor="address" className={LABEL_CLASSES}>
          Dirección
        </label>
        <input
          id="address"
          name="address"
          type="text"
          maxLength={200}
          defaultValue={defaultValues?.address ?? ''}
          className={INPUT_CLASSES}
        />
      </div>

      <div>
        <label htmlFor="phone" className={LABEL_CLASSES}>
          Teléfono
        </label>
        <input
          id="phone"
          name="phone"
          type="tel"
          maxLength={30}
          defaultValue={defaultValues?.phone ?? ''}
          className={INPUT_CLASSES}
        />
      </div>

      <div>
        <label htmlFor="contactEmail" className={LABEL_CLASSES}>
          Email de contacto
        </label>
        <input
          id="contactEmail"
          name="contactEmail"
          type="email"
          maxLength={150}
          defaultValue={defaultValues?.contactEmail ?? ''}
          className={INPUT_CLASSES}
        />
      </div>

      <div>
        <label htmlFor="logoUrl" className={LABEL_CLASSES}>
          URL del logo
        </label>
        <input
          id="logoUrl"
          name="logoUrl"
          type="url"
          maxLength={500}
          defaultValue={defaultValues?.logoUrl ?? ''}
          className={INPUT_CLASSES}
        />
      </div>
    </fieldset>
  )
}
