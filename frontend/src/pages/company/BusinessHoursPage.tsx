import { useState } from 'react'
import type { FormEvent } from 'react'
import { useBusinessHours } from '../../hooks/useBusinessHours'
import { useSaveBusinessHours } from '../../hooks/useSaveBusinessHours'
import { DAY_OF_WEEK_LABELS, DAYS_OF_WEEK } from '../../lib/daysOfWeek'
import {
  ERROR_CLASSES,
  FIELDSET_CLASSES,
  INPUT_CLASSES,
  LABEL_CLASSES,
  MUTED_TEXT_CLASSES,
  PAGE_TITLE_CLASSES,
  PRIMARY_BUTTON_CLASSES,
  SECONDARY_BUTTON_CLASSES,
  SUCCESS_CLASSES,
} from '../../lib/formStyles'
import { handleInvalidField, resetFieldValidity } from '../../lib/formValidation'
import type { BusinessHours, DayOfWeek } from '../../types/BusinessHours'

// Franja mientras se edita. La key identifica la fila para React; el intervalo es texto para poder borrarlo
// y escribir otro número sin que aparezca un 0.
interface EditableSlot {
  key: string
  dayOfWeek: DayOfWeek
  startTime: string
  endTime: string
  intervalMinutes: string
}

type SlotField = 'startTime' | 'endTime' | 'intervalMinutes'

// El backend manda '09:00:00' y el input de hora espera '09:00'.
function toEditableSlot(hours: BusinessHours): EditableSlot {
  return {
    key: crypto.randomUUID(),
    dayOfWeek: hours.dayOfWeek,
    startTime: hours.startTime.slice(0, 5),
    endTime: hours.endTime.slice(0, 5),
    intervalMinutes: String(hours.intervalMinutes),
  }
}

// Error de cada franja, por su key.
type SlotErrors = Record<string, string>

// Las mismas reglas que ScheduleService.validate en el backend, pero juntando todos los errores para mostrar
// cada uno debajo de su franja. Las horas 'HH:mm' se comparan como texto. Dos franjas que se tocan
// (09:00–13:00 y 13:00–17:00) no se superponen.
function findSlotErrors(slots: EditableSlot[]): SlotErrors {
  const errors: SlotErrors = {}
  for (const day of DAYS_OF_WEEK) {
    const daySlots = slots.filter((slot) => slot.dayOfWeek === day)

    for (const slot of daySlots) {
      if (slot.endTime <= slot.startTime) {
        errors[slot.key] = 'La hora de fin tiene que ser posterior a la de inicio.'
      }
    }

    // Una franja sin rango válido no se compara. Cada una se compara con la que termina más tarde de las
    // anteriores, así una franja larga detecta todas las que se le meten adentro.
    const sorted = daySlots
      .filter((slot) => slot.endTime > slot.startTime)
      .sort((a, b) => a.startTime.localeCompare(b.startTime))
    let latest: EditableSlot | null = null
    for (const slot of sorted) {
      if (latest && slot.startTime < latest.endTime) {
        errors[slot.key] =
          `Se superpone con la franja de ${latest.startTime} a ${latest.endTime}.`
      }
      if (!latest || slot.endTime > latest.endTime) {
        latest = slot
      }
    }
  }
  return errors
}

export function BusinessHoursPage() {
  const { hours, error, isLoading } = useBusinessHours()

  if (isLoading) {
    return <p className={MUTED_TEXT_CLASSES}>Cargando...</p>
  }

  if (error) {
    return (
      <p role="alert" className={ERROR_CLASSES}>
        {error}
      </p>
    )
  }

  return <BusinessHoursForm initialHours={hours} />
}

function BusinessHoursForm({ initialHours }: { initialHours: BusinessHours[] }) {
  const [slots, setSlots] = useState(() => initialHours.map(toEditableSlot))
  const [slotErrors, setSlotErrors] = useState<SlotErrors>({})
  const [hasChanges, setHasChanges] = useState(false)
  const { save, saved, error, isLoading, reset } = useSaveBusinessHours()
  const hasSlotErrors = Object.keys(slotErrors).length > 0

  // Al cambiar el horario aparece el botón de guardar y se borran el resultado del último guardado y los
  // errores de cada franja.
  function markChanged() {
    setHasChanges(true)
    reset()
    setSlotErrors({})
  }

  function addSlot(day: DayOfWeek) {
    markChanged()
    setSlots((current) => [
      ...current,
      {
        key: crypto.randomUUID(),
        dayOfWeek: day,
        startTime: '09:00',
        endTime: '18:00',
        intervalMinutes: '30',
      },
    ])
  }

  function updateSlot(key: string, field: SlotField, value: string) {
    markChanged()
    setSlots((current) =>
      current.map((slot) => (slot.key === key ? { ...slot, [field]: value } : slot)),
    )
  }

  function removeSlot(key: string) {
    markChanged()
    setSlots((current) => current.filter((slot) => slot.key !== key))
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const errors = findSlotErrors(slots)
    setSlotErrors(errors)
    if (Object.keys(errors).length > 0) {
      return
    }
    save(
      slots.map(({ dayOfWeek, startTime, endTime, intervalMinutes }) => ({
        dayOfWeek,
        startTime,
        endTime,
        intervalMinutes: Number(intervalMinutes),
      })),
    )
  }

  return (
    <form
      className="space-y-6"
      onSubmit={handleSubmit}
      onInvalidCapture={handleInvalidField}
      onInputCapture={resetFieldValidity}
    >
      <div className="space-y-1">
        <h2 className={PAGE_TITLE_CLASSES}>Horario semanal</h2>
        <p className={MUTED_TEXT_CLASSES}>
          Un día sin franjas queda cerrado. Si cerrás a medianoche, poné 23:59.
        </p>
      </div>

      {DAYS_OF_WEEK.map((day) => {
        const daySlots = slots.filter((slot) => slot.dayOfWeek === day)
        return (
          <fieldset key={day} className={FIELDSET_CLASSES}>
            <legend className="px-2 text-sm font-semibold text-ink">
              {DAY_OF_WEEK_LABELS[day]}
            </legend>

            {daySlots.length === 0 && <p className={MUTED_TEXT_CLASSES}>Cerrado</p>}

            {daySlots.map((slot) => (
              <div key={slot.key} className="space-y-2">
                <div className="grid items-end gap-3 sm:grid-cols-[1fr_1fr_1fr_auto]">
                  <div>
                    <label htmlFor={`${slot.key}-start`} className={LABEL_CLASSES}>
                      Desde *
                    </label>
                    <input
                      id={`${slot.key}-start`}
                      type="time"
                      required
                      value={slot.startTime}
                      onChange={(event) => updateSlot(slot.key, 'startTime', event.target.value)}
                      className={INPUT_CLASSES}
                    />
                  </div>
                  <div>
                    <label htmlFor={`${slot.key}-end`} className={LABEL_CLASSES}>
                      Hasta *
                    </label>
                    <input
                      id={`${slot.key}-end`}
                      type="time"
                      required
                      value={slot.endTime}
                      onChange={(event) => updateSlot(slot.key, 'endTime', event.target.value)}
                      className={INPUT_CLASSES}
                    />
                  </div>
                  <div>
                    <label htmlFor={`${slot.key}-interval`} className={LABEL_CLASSES}>
                      Intervalo (min) *
                    </label>
                    <input
                      id={`${slot.key}-interval`}
                      type="number"
                      required
                      min={1}
                      max={1440}
                      step={1}
                      value={slot.intervalMinutes}
                      onChange={(event) =>
                        updateSlot(slot.key, 'intervalMinutes', event.target.value)
                      }
                      className={INPUT_CLASSES}
                    />
                  </div>
                  <button
                    type="button"
                    onClick={() => removeSlot(slot.key)}
                    className={SECONDARY_BUTTON_CLASSES}
                  >
                    Quitar
                  </button>
                </div>
                {slotErrors[slot.key] && (
                  <p role="alert" className={ERROR_CLASSES}>
                    {slotErrors[slot.key]}
                  </p>
                )}
              </div>
            ))}

            <button
              type="button"
              onClick={() => addSlot(day)}
              className={SECONDARY_BUTTON_CLASSES}
            >
              Agregar franja
            </button>
          </fieldset>
        )
      })}

      {hasSlotErrors && (
        <p role="alert" className={ERROR_CLASSES}>
          Hay errores en el horario. Revisá los días marcados.
        </p>
      )}

      {error && (
        <p role="alert" className={ERROR_CLASSES}>
          {error}
        </p>
      )}

      {saved && (
        <p role="status" className={SUCCESS_CLASSES}>
          Horario guardado
        </p>
      )}

      {/* Solo con cambios sin guardar: si no, la pantalla muestra el horario como está. */}
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
