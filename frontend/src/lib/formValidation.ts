import type { FormEvent } from 'react'

type FormField = HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement

// Mensaje en español para el primer problema del campo. Los mensajes nativos del navegador salen en su idioma.
function getValidationMessage(field: FormField): string {
  const { validity } = field

  if (validity.valueMissing) {
    return field instanceof HTMLSelectElement
      ? 'Elegí una opción de la lista.'
      : 'Completá este campo.'
  }
  if (validity.typeMismatch && field.type === 'email') {
    return 'Ingresá un email válido, por ejemplo nombre@dominio.com.'
  }
  if (validity.typeMismatch && field.type === 'url') {
    return 'Ingresá una dirección válida que empiece con https://.'
  }
  if (validity.tooLong) {
    return 'El texto es demasiado largo.'
  }
  return 'Revisá este campo.'
}

/**
 * Handlers para pasar a un <form> como onInvalidCapture y onInputCapture: reemplazan los mensajes
 * nativos del navegador por mensajes en español sin desactivar la validación nativa.
 */
export function handleInvalidField(event: FormEvent<HTMLFormElement>): void {
  const field = event.target as FormField
  // Un error propio (setCustomValidity desde el componente) ya trae su texto.
  if (field.validity.customError) {
    return
  }
  field.setCustomValidity(getValidationMessage(field))
}

export function resetFieldValidity(event: FormEvent<HTMLFormElement>): void {
  const field = event.target as FormField
  field.setCustomValidity('')
}
