import type { FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { ScheduleExceptionFields } from '../../components/ScheduleExceptionFields'
import { useRedirectAfterSuccess } from '../../hooks/useRedirectAfterSuccess'
import { useSaveScheduleException } from '../../hooks/useSaveScheduleException'
import { useScheduleExceptions } from '../../hooks/useScheduleExceptions'
import {
  ERROR_CLASSES,
  LINK_CLASSES,
  MUTED_TEXT_CLASSES,
  PAGE_TITLE_CLASSES,
  PRIMARY_BUTTON_CLASSES,
  SUCCESS_CLASSES,
} from '../../lib/formStyles'
import { handleInvalidField, resetFieldValidity } from '../../lib/formValidation'
import { readScheduleExceptionData } from '../../lib/scheduleExceptionForm'

// La key remonta la pantalla al cambiar de excepción: así no se arrastra el estado del guardado anterior.
export function EditScheduleExceptionPage() {
  const { id = '' } = useParams()
  return <EditScheduleExceptionForm key={id} id={id} />
}

function EditScheduleExceptionForm({ id }: { id: string }) {
  // No hay GET de una sola excepción: se busca en el listado, que ya trae todas las que se pueden editar.
  const { exceptions, error: loadError, isLoading: isLoadingList } = useScheduleExceptions()
  const exception = exceptions.find((item) => item.id === id)
  const { save, saved, error, isLoading, reset } = useSaveScheduleException(id)
  // Guardada: muestra el aviso y a los pocos segundos vuelve al listado.
  useRedirectAfterSuccess(saved, '/company/schedule-exceptions')

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    save(readScheduleExceptionData(new FormData(event.currentTarget)))
  }

  if (isLoadingList) {
    return <p className={MUTED_TEXT_CLASSES}>Cargando...</p>
  }

  if (loadError) {
    return (
      <p role="alert" className={ERROR_CLASSES}>
        {loadError}
      </p>
    )
  }

  if (!exception) {
    return (
      <section className="space-y-4">
        <p className="text-sm text-body">La excepción no existe</p>
        <Link to="/company/schedule-exceptions" className={LINK_CLASSES}>
          Volver al listado
        </Link>
      </section>
    )
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
        <h2 className={PAGE_TITLE_CLASSES}>Editar excepción</h2>
        <Link to="/company/schedule-exceptions" className={LINK_CLASSES}>
          Volver al listado
        </Link>
      </div>

      <ScheduleExceptionFields defaultValues={exception} />

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

      <button
        type="submit"
        disabled={isLoading || saved}
        className={`w-full ${PRIMARY_BUTTON_CLASSES}`}
      >
        {isLoading ? 'Guardando...' : 'Guardar cambios'}
      </button>
    </form>
  )
}
