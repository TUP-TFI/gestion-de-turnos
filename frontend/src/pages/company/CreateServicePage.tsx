import type { FormEvent } from 'react'
import { Link } from 'react-router'
import { ServiceFields } from '../../components/ServiceFields'
import { useCreateService } from '../../hooks/useCreateService'
import { useRedirectAfterSuccess } from '../../hooks/useRedirectAfterSuccess'
import {
  ERROR_CLASSES,
  LINK_CLASSES,
  PAGE_TITLE_CLASSES,
  PRIMARY_BUTTON_CLASSES,
  SUCCESS_CLASSES,
} from '../../lib/formStyles'
import { handleInvalidField, resetFieldValidity } from '../../lib/formValidation'
import { readServiceData } from '../../lib/serviceForm'

export function CreateServicePage() {
  const { createService, createdService, error, isLoading, reset } =
    useCreateService()
  // Creado: muestra el aviso y a los pocos segundos vuelve al listado.
  useRedirectAfterSuccess(Boolean(createdService), '/company/services')

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    createService(readServiceData(new FormData(event.currentTarget)))
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
        <h2 className={PAGE_TITLE_CLASSES}>Nuevo servicio</h2>
        <Link to="/company/services" className={LINK_CLASSES}>
          Volver al listado
        </Link>
      </div>

      <ServiceFields />

      {error && (
        <p role="alert" className={ERROR_CLASSES}>
          {error}
        </p>
      )}

      {createdService && (
        <p role="status" className={SUCCESS_CLASSES}>
          Servicio creado
        </p>
      )}

      <button
        type="submit"
        disabled={isLoading || Boolean(createdService)}
        className={`w-full ${PRIMARY_BUTTON_CLASSES}`}
      >
        {isLoading ? 'Creando...' : 'Crear servicio'}
      </button>
    </form>
  )
}
