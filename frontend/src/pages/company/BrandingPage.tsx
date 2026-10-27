import { useState } from 'react'
import type { FormEvent } from 'react'
import { useBranding } from '../../hooks/useBranding'
import { useUpdateBranding } from '../../hooks/useUpdateBranding'
import { readBrandingData } from '../../lib/brandingForm'
import {
  ERROR_CLASSES,
  FIELDSET_CLASSES,
  INPUT_CLASSES,
  LABEL_CLASSES,
  LINK_CLASSES,
  MUTED_TEXT_CLASSES,
  PAGE_TITLE_CLASSES,
  PRIMARY_BUTTON_CLASSES,
  SUCCESS_CLASSES,
} from '../../lib/formStyles'
import { handleInvalidField, resetFieldValidity } from '../../lib/formValidation'
import type { CompanyBranding } from '../../types/CompanyBranding'

// Mismo formato que el CHECK de la base: # y 6 dígitos hexadecimales.
const COLOR_PATTERN = '#[0-9A-Fa-f]{6}'
const COLOR_REGEX = new RegExp(`^${COLOR_PATTERN}$`)

export function BrandingPage() {
  const { branding, error, isLoading } = useBranding()

  if (isLoading) {
    return <p className={MUTED_TEXT_CLASSES}>Cargando...</p>
  }

  if (!branding) {
    return (
      <p role="alert" className={ERROR_CLASSES}>
        {error}
      </p>
    )
  }

  return <BrandingForm initial={branding} />
}

function BrandingForm({ initial }: { initial: CompanyBranding }) {
  // Logo y color son controlados para que la vista previa cambie mientras el admin escribe.
  const [logoUrl, setLogoUrl] = useState(initial.logoUrl ?? '')
  const [primaryColor, setPrimaryColor] = useState(initial.primaryColor ?? '')
  const { updateBranding, saved, error, isLoading, reset } = useUpdateBranding()
  const isValidColor = COLOR_REGEX.test(primaryColor)
  // Hay cambios si lo escrito es distinto de lo guardado. Al guardar, useUpdateBranding vuelve a pedir la marca
  // y `initial` llega con los valores nuevos.
  const hasChanges =
    logoUrl !== (initial.logoUrl ?? '') || primaryColor !== (initial.primaryColor ?? '')

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    updateBranding(readBrandingData(new FormData(event.currentTarget)))
  }

  return (
    <form
      className="space-y-6"
      onSubmit={handleSubmit}
      onInvalidCapture={handleInvalidField}
      onInputCapture={(event) => {
        resetFieldValidity(event)
        reset()
      }}
    >
      <div className="space-y-1">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className={PAGE_TITLE_CLASSES}>Marca</h2>
          {/* Se abre en otra pestaña para no perder lo que el admin está editando. */}
          <a href={`/${initial.slug}`} target="_blank" rel="noreferrer" className={LINK_CLASSES}>
            Ver mi página pública
          </a>
        </div>
        <p className={MUTED_TEXT_CLASSES}>
          El logo y el color se muestran en tu página pública. Si los dejás vacíos, se usa el estilo por defecto.
        </p>
      </div>

      <fieldset className={FIELDSET_CLASSES}>
        <legend className="px-2 text-sm font-semibold text-ink">Logo y color</legend>

        <div>
          <label htmlFor="logoUrl" className={LABEL_CLASSES}>
            URL del logo
          </label>
          <input
            id="logoUrl"
            name="logoUrl"
            type="url"
            maxLength={500}
            value={logoUrl}
            onChange={(event) => setLogoUrl(event.target.value)}
            className={INPUT_CLASSES}
          />
          <p className={`mt-1 ${MUTED_TEXT_CLASSES}`}>
            La dirección de una imagen ya subida a internet, que empiece con https://.
          </p>
        </div>

        <div>
          <label htmlFor="primaryColor" className={LABEL_CLASSES}>
            Color principal
          </label>
          <div className="flex items-center gap-3">
            {/* El selector no admite "sin color": arranca en negro si el texto está vacío o incompleto. */}
            <input
              type="color"
              aria-label="Elegir color"
              value={isValidColor ? primaryColor : '#000000'}
              onChange={(event) => setPrimaryColor(event.target.value.toUpperCase())}
              className="h-10 w-12 cursor-pointer"
            />
            <input
              id="primaryColor"
              name="primaryColor"
              type="text"
              pattern={COLOR_PATTERN}
              maxLength={7}
              placeholder="#1A73E8"
              value={primaryColor}
              onChange={(event) => setPrimaryColor(event.target.value)}
              className={INPUT_CLASSES}
            />
          </div>
          <p className={`mt-1 ${MUTED_TEXT_CLASSES}`}>
            Elegilo con el selector o escribilo con el formato #RRGGBB, por ejemplo #1A73E8.
          </p>
        </div>
      </fieldset>

      <fieldset className={FIELDSET_CLASSES}>
        <legend className="px-2 text-sm font-semibold text-ink">Vista previa</legend>
        {logoUrl ? (
          <img src={logoUrl} alt="Logo de la empresa" className="h-16 w-auto object-contain" />
        ) : (
          <p className={MUTED_TEXT_CLASSES}>Sin logo</p>
        )}
        {/* Muestra de cómo se ve un botón con el color elegido. No hace nada al tocarlo. */}
        <span
          className={`inline-block ${PRIMARY_BUTTON_CLASSES}`}
          style={isValidColor ? { backgroundColor: primaryColor } : undefined}
        >
          Reservar turno
        </span>
      </fieldset>

      {error && (
        <p role="alert" className={ERROR_CLASSES}>
          {error}
        </p>
      )}

      {saved && (
        <p role="status" className={SUCCESS_CLASSES}>
          Cambios guardados
        </p>
      )}

      {/* Solo con cambios sin guardar, igual que en el horario semanal. */}
      {hasChanges && !saved && (
        <button
          type="submit"
          disabled={isLoading}
          className={`w-full ${PRIMARY_BUTTON_CLASSES}`}
        >
          {isLoading ? 'Guardando...' : 'Guardar cambios'}
        </button>
      )}
    </form>
  )
}
