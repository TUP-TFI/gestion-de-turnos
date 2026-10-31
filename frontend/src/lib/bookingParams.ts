// Servicio y horario elegidos antes de iniciar sesión. Viajan en la URL para no perderse al pasar por el login o el
// registro.
interface BookingParams {
  serviceId: string
  start: string
}

export function toBookingSearch({ serviceId, start }: BookingParams): string {
  return `?${new URLSearchParams({ serviceId, start })}`
}

// Null si la URL no los trae completos o si la fecha no es válida: la URL se puede editar a mano.
export function readBookingParams(search: string): BookingParams | null {
  const params = new URLSearchParams(search)
  const serviceId = params.get('serviceId')
  const start = params.get('start')

  if (!serviceId || !start || Number.isNaN(Date.parse(start))) {
    return null
  }
  return { serviceId, start }
}
