import type { FormEvent } from 'react'
import { Link } from 'react-router'
import { ScheduleExceptionFields } from '../../components/ScheduleExceptionFields'
import { useRedirectAfterSuccess } from '../../hooks/useRedirectAfterSuccess'
import { useSaveScheduleException } from '../../hooks/useSaveScheduleException'
import {
  ERROR_CLASSES,
  LINK_CLASSES,
  PAGE_TITLE_CLASSES,
  PRIMARY_BUTTON_CLASSES,
  SUCCESS_CLASSES,
} from '../../lib/formStyles'
import { handleInvalidField, resetFieldValidity } from '../../lib/formValidation'
import { readScheduleExceptionData } from '../../lib/scheduleExceptionForm'

export function CreateScheduleExceptionPage() {
  const { save, saved, error, isLoading, reset } = useSaveScheduleException()
  // Creada: muestra el aviso y a los pocos segundos vuelve al listado.
  useRedirectAfterSuccess(saved, '/company/schedule-exceptions')

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    save(readScheduleExceptionData(new FormData(event.currentTarget)))
  }

  return (
    <form
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
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className={PAGE_TITLE_CLASSES}>Nueva excepción</h2>
        <Link to="/company/schedule-exceptions" className={LINK_CLASSES}>
          Volver al listado
        </Link>
      </div>

      <ScheduleExceptionFields />

      {error && (
        <p role="alert" className={ERROR_CLASSES}>
          {error}
        </p>
      )}

      {saved && (
        <p role="status" className={SUCCESS_CLASSES}>
          Excepción creada
        </p>
      )}

      <button
        type="submit"
        disabled={isLoading || saved}
        className={`w-full ${PRIMARY_BUTTON_CLASSES}`}
      >
        {isLoading ? 'Creando...' : 'Crear excepción'}
      </button>
    </form>
  )
}
