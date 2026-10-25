import type { ServiceData } from '../types/Service'
import { toNullable, toRequired } from './companyForm'

// Precio en formato argentino (1.500,99) a número (1500.99): saca los puntos de miles y la coma pasa a punto.
function parsePrice(value: FormDataEntryValue | null): number {
  return Number(toRequired(value).replace(/\./g, '').replace(',', '.'))
}

// Arma los datos del servicio desde el form de ServiceFields. Precio y duración son required: nunca llegan vacíos.
export function readServiceData(data: FormData): ServiceData {
  return {
    name: toRequired(data.get('name')),
    description: toNullable(data.get('description')),
    price: parsePrice(data.get('price')),
    durationMinutes: Number(toRequired(data.get('durationMinutes'))),
  }
}
