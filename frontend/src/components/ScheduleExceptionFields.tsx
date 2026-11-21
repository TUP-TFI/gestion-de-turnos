import { useState } from 'react'
import { IntervalField } from './IntervalField'
import {
  FIELDSET_CLASSES,
  INPUT_CLASSES,
  LABEL_CLASSES,
  MUTED_TEXT_CLASSES,
} from '../lib/formStyles'
import {
  FULL_DAY_END,
  FULL_DAY_LABEL,
  FULL_DAY_START,
  isFullDay,
} from '../lib/fullDay'
import { describeStarts } from '../lib/slotInterval'
import type { ScheduleExceptionData } from '../types/ScheduleException'

interface ScheduleExceptionFieldsProps {
  // Valores iniciales para la edición. Al enviar, los campos se leen con readScheduleExceptionData. El horario y
  // el intervalo además tienen estado, para la vista previa de los turnos.
  defaultValues?: ScheduleExceptionData
}

// Campos de la excepción que comparten el alta y la edición. Con "Cerrado todo el día" marcado, los campos
// de horario no se renderizan, así no viajan en el form.
export function ScheduleExceptionFields({ defaultValues }: ScheduleExceptionFieldsProps) {
  // Un alta arranca como día cerrado, el caso más común (feriado).
  const [isClosed, setIsClosed] = useState(defaultValues?.isClosed ?? true)
  const wasFullDay = isFullDay(defaultValues?.startTime ?? null, defaultValues?.endTime ?? null)
  const [isOpenAllDay, setIsOpenAllDay] = useState(wasFullDay)
  // Si la excepción ya era de 24 horas y se desmarca, los campos arrancan con el horario por defecto y no con
  // 00:00 a 23:59.
  const [startTime, setStartTime] = useState(
    wasFullDay ? '09:00' : (defaultValues?.startTime?.slice(0, 5) ?? '09:00'),
  )
  const [endTime, setEndTime] = useState(
    wasFullDay ? '18:00' : (defaultValues?.endTime?.slice(0, 5) ?? '18:00'),
  )
  const [intervalMinutes, setIntervalMinutes] = useState(String(defaultValues?.intervalMinutes ?? 30))
  const preview = isOpenAllDay
    ? describeStarts(FULL_DAY_START, FULL_DAY_END, intervalMinutes)
    : describeStarts(startTime, endTime, intervalMinutes)

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
          {/* Sin name: no viaja en el form. El horario de 24 horas sale de los dos campos ocultos de abajo. */}
          <div className="flex items-center gap-2 sm:col-span-3">
            <input
              id="isOpenAllDay"
              type="checkbox"
              checked={isOpenAllDay}
              onChange={(event) => setIsOpenAllDay(event.target.checked)}
              className="h-4 w-4"
            />
            <label htmlFor="isOpenAllDay" className="text-sm text-ink">
              {FULL_DAY_LABEL}
            </label>
          </div>
          {isOpenAllDay ? (
            <>
              <input type="hidden" name="startTime" value={FULL_DAY_START} />
              <input type="hidden" name="endTime" value={FULL_DAY_END} />
            </>
          ) : (
            <>
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
                  value={startTime}
                  onChange={(event) => setStartTime(event.target.value)}
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
                  value={endTime}
                  onChange={(event) => setEndTime(event.target.value)}
                  className={INPUT_CLASSES}
                />
              </div>
            </>
          )}
          <IntervalField
            id="intervalMinutes"
            name="intervalMinutes"
            value={intervalMinutes}
            onChange={setIntervalMinutes}
          />
          {preview && <p className={`sm:col-span-3 ${MUTED_TEXT_CLASSES}`}>{preview}</p>}
          <p className={`sm:col-span-3 ${MUTED_TEXT_CLASSES}`}>
            Ese día se usa este horario en lugar del semanal.
            {!isOpenAllDay && ' Si cerrás a medianoche, poné 23:59.'} Un servicio que dura más que el tiempo
            entre turnos usa varios seguidos.
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
