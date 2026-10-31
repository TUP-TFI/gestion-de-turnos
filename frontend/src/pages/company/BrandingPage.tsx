import { useState } from 'react'
import type { FormEvent } from 'react'
import { useBranding } from '../../hooks/useBranding'
import { useUpdateBranding } from '../../hooks/useUpdateBranding'
import { readBrandingData } from '../../lib/brandingForm'
import { companyThemeStyle } from '../../lib/companyTheme'
import {
  COMPANY_BAND_CLASSES,
  ERROR_CLASSES,
  FIELDSET_CLASSES,
  INPUT_CLASSES,
  LABEL_CLASSES,
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

// Si la imagen no carga, se oculta en lugar de mostrar el ícono roto. El formulario le pone una key con la URL, para
// que una dirección nueva vuelva a intentar. El alt va vacío porque es solo una muestra.
function PreviewLogo({ url }: { url: string }) {
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
          <a
            href={`/${initial.slug}`}
            target="_blank"
            rel="noreferrer"
            className={PRIMARY_BUTTON_CLASSES}
          >
            Ver mi página pública ↗
          </a>
        </div>
        <p className={MUTED_TEXT_CLASSES}>
          El logo y el color se muestran en tu página pública. Si los dejás vacíos, se usa el estilo por defecto.
        </p>
      </div>

      {/* Desde lg, formulario a la izquierda y vista previa a la derecha; en el celular, uno abajo del otro. */}
      <div className="grid items-start gap-6 lg:grid-cols-2">
        <fieldset className={`min-w-0 ${FIELDSET_CLASSES}`}>
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

        <fieldset className={`min-w-0 ${FIELDSET_CLASSES}`}>
          <legend className="px-2 text-sm font-semibold text-ink">Vista previa</legend>
          <div
            className="space-y-4"
            style={companyThemeStyle(isValidColor ? primaryColor : null)}
          >
            {/* Mini encabezado de la página pública. CompanyBranding no trae el nombre: va un texto fijo. */}
            <div className={`flex items-center gap-4 rounded-lg p-4 ${COMPANY_BAND_CLASSES}`}>
              {logoUrl && <PreviewLogo key={logoUrl} url={logoUrl} />}
              <p className="text-[22px] font-semibold">Tu empresa</p>
            </div>
            {/* Muestra del botón con el color elegido: es un span porque no hace nada al tocarlo. */}
            <div className="space-y-1">
              <span className="inline-flex h-10 cursor-default items-center justify-center rounded-lg bg-(--company-accent) px-4 text-sm font-semibold text-(--company-on-accent)">
                Reservar turno
              </span>
              <p className={MUTED_TEXT_CLASSES}>
                Así se verá el botón en tu página. Acá no se puede tocar.
              </p>
            </div>
          </div>
        </fieldset>
      </div>

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
