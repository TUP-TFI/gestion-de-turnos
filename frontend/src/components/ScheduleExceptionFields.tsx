import { useState } from 'react'
import {
  FIELDSET_CLASSES,
  INPUT_CLASSES,
  LABEL_CLASSES,
  MUTED_TEXT_CLASSES,
} from '../lib/formStyles'
import type { ScheduleExceptionData } from '../types/ScheduleException'

interface ScheduleExceptionFieldsProps {
  // Valores iniciales para la edición. Los inputs no son controlados: se leen con readScheduleExceptionData
  // al enviar.
  defaultValues?: ScheduleExceptionData
}

// Campos de la excepción que comparten el alta y la edición. Con "Cerrado todo el día" marcado, los campos
// de horario no se renderizan, así no viajan en el form.
export function ScheduleExceptionFields({ defaultValues }: ScheduleExceptionFieldsProps) {
  // Un alta arranca como día cerrado, el caso más común (feriado).
  const [isClosed, setIsClosed] = useState(defaultValues?.isClosed ?? true)

  return (
    <fieldset className={FIELDSET_CLASSES}>
      <legend className="px-2 text-sm font-semibold text-ink">Datos del día</legend>

      <div>
        <label htmlFor="exceptionDate" className={LABEL_CLASSES}>
          Fecha *
        </label>
        <input
          id="exceptionDate"
          name="exceptionDate"
          type="date"
          required
          defaultValue={defaultValues?.exceptionDate}
          className={INPUT_CLASSES}
        />
      </div>

      <div className="flex items-center gap-2">
        <input
          id="isClosed"
          name="isClosed"
          type="checkbox"
          checked={isClosed}
          onChange={(event) => setIsClosed(event.target.checked)}
          className="h-4 w-4"
        />
        <label htmlFor="isClosed" className="text-sm text-ink">
          Cerrado todo el día
        </label>
      </div>

      {!isClosed && (
        <div className="grid gap-3 sm:grid-cols-3">
          <p className="text-sm font-medium text-ink sm:col-span-3">
            Elegí el horario en el que vas a tomar turnos ese día
          </p>
          <div>
            <label htmlFor="startTime" className={LABEL_CLASSES}>
              Desde *
            </label>
            {/* El backend manda '10:00:00' y el input de hora espera '10:00'. */}
            <input
              id="startTime"
              name="startTime"
              type="time"
              required
              defaultValue={defaultValues?.startTime?.slice(0, 5) ?? '09:00'}
              className={INPUT_CLASSES}
            />
          </div>
          <div>
            <label htmlFor="endTime" className={LABEL_CLASSES}>
              Hasta *
            </label>
            <input
              id="endTime"
              name="endTime"
              type="time"
              required
              defaultValue={defaultValues?.endTime?.slice(0, 5) ?? '18:00'}
              className={INPUT_CLASSES}
            />
          </div>
          <div>
            <label htmlFor="intervalMinutes" className={LABEL_CLASSES}>
              Intervalo (min) *
            </label>
            <input
              id="intervalMinutes"
              name="intervalMinutes"
              type="number"
              required
              min={1}
              max={1440}
              step={1}
              defaultValue={defaultValues?.intervalMinutes ?? 30}
              className={INPUT_CLASSES}
            />
          </div>
          <p className={`sm:col-span-3 ${MUTED_TEXT_CLASSES}`}>
            Ese día se usa este horario en lugar del semanal. Si cerrás a medianoche, poné 23:59.
          </p>
        </div>
      )}

      <div>
        <label htmlFor="reason" className={LABEL_CLASSES}>
          Motivo
        </label>
        <input
          id="reason"
          name="reason"
          type="text"
          maxLength={200}
          placeholder="Por ejemplo: Navidad"
          defaultValue={defaultValues?.reason ?? ''}
          className={INPUT_CLASSES}
        />
      </div>
    </fieldset>
  )
}
