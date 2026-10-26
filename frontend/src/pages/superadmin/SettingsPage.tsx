import type { FormEvent } from 'react'
import { usePlatformSettings } from '../../hooks/usePlatformSettings'
import { useUpdatePlatformSettings } from '../../hooks/useUpdatePlatformSettings'
import {
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
import { readSettingsData } from '../../lib/settingsForm'
import type { PlatformSettingsData } from '../../types/PlatformSettings'

interface SettingField {
  name: keyof PlatformSettingsData
  label: string
  // Los min y max coinciden con las validaciones del back (PlatformSettingsRequest).
  min: number
  max: number
  help: string
}

const FIELDS: SettingField[] = [
  {
    name: 'minBookingNoticeMinutes',
    label: 'Anticipación mínima para reservar *',
    min: 1,
    max: 1440,
    help: 'En minutos. Un cliente no puede reservar un turno que empiece antes de este tiempo.',
  },
  {
    name: 'maxBookingAdvanceDays',
    label: 'Anticipación máxima para reservar *',
    min: 1,
    max: 365,
    help: 'En días. Un cliente no puede reservar un turno más lejano que esto.',
  },
  {
    name: 'cancellationDeadlineHours',
    label: 'Plazo para cancelar o reprogramar *',
    min: 1,
    max: 168,
    help: 'En horas antes del turno. Pasado ese plazo, el cliente ya no puede cancelar ni reprogramar.',
  },
  {
    name: 'maxRescheduleCount',
    label: 'Máximo de reprogramaciones por turno *',
    min: 0,
    max: 10,
    help: 'Cantidad. Con 0, los turnos no se pueden reprogramar.',
  },
]

export function SettingsPage() {
  const { settings, error: loadError, isLoading: isLoadingSettings } = usePlatformSettings()
  const { updateSettings, updatedSettings, error, isLoading, reset } =
    useUpdatePlatformSettings()

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    updateSettings(readSettingsData(new FormData(event.currentTarget)))
  }

  if (isLoadingSettings) {
    return <p className={MUTED_TEXT_CLASSES}>Cargando...</p>
  }

  if (!settings) {
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
        if (error || updatedSettings) {
          reset()
        }
      }}
    >
      <div>
        <h2 className={PAGE_TITLE_CLASSES}>Parámetros</h2>
        <p className={`mt-1 ${MUTED_TEXT_CLASSES}`}>
          Rigen de inmediato para todas las empresas. No afectan a los turnos ya reservados.
        </p>
      </div>

      <fieldset className={FIELDSET_CLASSES}>
        {FIELDS.map((field) => (
          <div key={field.name}>
            <label htmlFor={field.name} className={LABEL_CLASSES}>
              {field.label}
            </label>
            <input
              id={field.name}
              name={field.name}
              type="number"
              required
              min={field.min}
              max={field.max}
              step={1}
              defaultValue={settings[field.name]}
              className={INPUT_CLASSES}
            />
            <p className={`mt-1 ${MUTED_TEXT_CLASSES}`}>{field.help}</p>
          </div>
        ))}
      </fieldset>

      {error && (
        <p role="alert" className={ERROR_CLASSES}>
          {error}
        </p>
      )}

      {updatedSettings && (
        <p role="status" className={SUCCESS_CLASSES}>
          Cambios guardados
        </p>
      )}

      <button
        type="submit"
        disabled={isLoading}
        className={`w-full ${PRIMARY_BUTTON_CLASSES}`}
      >
        {isLoading ? 'Guardando...' : 'Guardar cambios'}
      </button>
    </form>
  )
}
