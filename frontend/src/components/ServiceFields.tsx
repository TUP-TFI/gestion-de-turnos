import { FIELDSET_CLASSES, INPUT_CLASSES, LABEL_CLASSES, MUTED_TEXT_CLASSES, TEXTAREA_CLASSES } from '../lib/formStyles'
import type { ServiceData } from '../types/Service'

interface ServiceFieldsProps {
  // Valores iniciales para la edición. Los inputs no son controlados: se leen con readServiceData al enviar.
  defaultValues?: ServiceData
}

// Campos del servicio que comparten el alta y la edición. Los límites son los de ServiceRequest en el backend.
export function ServiceFields({ defaultValues }: ServiceFieldsProps) {
  return (
    <fieldset className={FIELDSET_CLASSES}>
      <legend className="px-2 text-sm font-semibold text-ink">
        Datos del servicio
      </legend>

      <div>
        <label htmlFor="name" className={LABEL_CLASSES}>
          Nombre *
        </label>
        <input
          id="name"
          name="name"
          type="text"
          required
          maxLength={120}
          defaultValue={defaultValues?.name}
          className={INPUT_CLASSES}
        />
      </div>

      <div>
        <label htmlFor="description" className={LABEL_CLASSES}>
          Descripción
        </label>
        <textarea
          id="description"
          name="description"
          rows={3}
          defaultValue={defaultValues?.description ?? ''}
          className={TEXTAREA_CLASSES}
        />
      </div>

      <div>
        <label htmlFor="price" className={LABEL_CLASSES}>
          Precio *
        </label>
        {/* Texto en vez de number: el input numérico no deja escribir la coma decimal ni el punto de miles.
            Acepta 1500, 1500,99, 1.500 y 1.500,99, con hasta 8 cifras enteras como @Digits del backend;
            readServiceData lo pasa a número. */}
        <input
          id="price"
          name="price"
          type="text"
          inputMode="decimal"
          required
          pattern="\d{1,8}(,\d{1,2})?|(\d{1,3}|\d{1,2}\.\d{3})\.\d{3}(,\d{1,2})?"
          defaultValue={
            defaultValues ? String(defaultValues.price).replace('.', ',') : undefined
          }
          className={INPUT_CLASSES}
        />
        <p className={`mt-1 ${MUTED_TEXT_CLASSES}`}>
          En pesos. Ejemplo: 1.500,99. Coma para los decimales.
        </p>
      </div>

      <div>
        <label htmlFor="durationMinutes" className={LABEL_CLASSES}>
          Duración en minutos *
        </label>
        <input
          id="durationMinutes"
          name="durationMinutes"
          type="number"
          required
          min={1}
          max={1440}
          step={1}
          defaultValue={defaultValues?.durationMinutes}
          className={INPUT_CLASSES}
        />
        <p className={`mt-1 ${MUTED_TEXT_CLASSES}`}>
          Cuánto dura el turno. Por ejemplo, 30 para media hora.
        </p>
      </div>
    </fieldset>
  )
}
