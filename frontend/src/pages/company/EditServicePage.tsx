import type { FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { ServiceFields } from '../../components/ServiceFields'
import { useRedirectAfterSuccess } from '../../hooks/useRedirectAfterSuccess'
import { useService } from '../../hooks/useService'
import { useUpdateService } from '../../hooks/useUpdateService'
import {
  ERROR_CLASSES,
  LINK_CLASSES,
  MUTED_TEXT_CLASSES,
  PAGE_TITLE_CLASSES,
  PRIMARY_BUTTON_CLASSES,
  SUCCESS_CLASSES,
} from '../../lib/formStyles'
import { handleInvalidField, resetFieldValidity } from '../../lib/formValidation'
import { readServiceData } from '../../lib/serviceForm'

// La key remonta la pantalla al cambiar de servicio: así no se arrastra el estado del guardado anterior.
export function EditServicePage() {
  const { id = '' } = useParams()
  return <EditServiceForm key={id} id={id} />
}

function EditServiceForm({ id }: { id: string }) {
  const { service, error: loadError, isLoading: isLoadingService, isNotFound } =
    useService(id)
  const { updateService, updatedService, error, isLoading, reset } =
    useUpdateService(id)
  // Guardado: muestra el aviso y a los pocos segundos vuelve al listado.
  useRedirectAfterSuccess(Boolean(updatedService), '/company/services')

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    updateService(readServiceData(new FormData(event.currentTarget)))
  }

  if (isLoadingService) {
    return <p className={MUTED_TEXT_CLASSES}>Cargando...</p>
  }

  if (isNotFound) {
    return (
      <section className="space-y-4">
        <p className="text-sm text-body">El servicio no existe</p>
        <Link to="/company/services" className={LINK_CLASSES}>
          Volver al listado
        </Link>
      </section>
    )
  }

  if (!service) {
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
        if (error) {
          reset()
        }
      }}
    >
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className={PAGE_TITLE_CLASSES}>Editar servicio</h2>
        <Link to="/company/services" className={LINK_CLASSES}>
          Volver al listado
        </Link>
      </div>

      <ServiceFields defaultValues={service} />

      {error && (
        <p role="alert" className={ERROR_CLASSES}>
          {error}
        </p>
      )}

      {updatedService && (
        <p role="status" className={SUCCESS_CLASSES}>
          Cambios guardados
        </p>
      )}

      <button
        type="submit"
        disabled={isLoading || Boolean(updatedService)}
        className={`w-full ${PRIMARY_BUTTON_CLASSES}`}
      >
        {isLoading ? 'Guardando...' : 'Guardar cambios'}
      </button>
    </form>
  )
}
