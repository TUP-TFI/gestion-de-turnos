import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiRequestError, toErrorMessage } from '../api/client'
import { createManualAppointment } from '../api/companyAppointments'
import type {
  AdminAppointment,
  ManualAppointmentRequest,
} from '../types/Appointment'

// El horario ya no se puede reservar: hay que elegir otro, con la disponibilidad al día.
const REJECTED_SLOT_CODES = [
  'SLOT_UNAVAILABLE',
  'BOOKING_TOO_SOON',
  'BOOKING_TOO_FAR',
]

interface CreateOptions {
  onSuccess?: (appointment: AdminAppointment) => void
  onError?: (code: string | null) => void
}

interface UseCreateManualAppointmentResult {
  create: (body: ManualAppointmentRequest, options?: CreateOptions) => void
  error: string | null
  // Código del error del backend, o null si no lo hay o no vino de la API.
  errorCode: string | null
  isLoading: boolean
  // Borra el error: por ejemplo, al cambiar un dato que lo dejó de explicar.
  reset: () => void
}

// Carga de un turno manual (US-09.5 y 09.6). Con éxito invalida la agenda y la disponibilidad. Con un horario
// rechazado, la disponibilidad; con un servicio que ya no existe, la lista de servicios. Lo demás, como soltar al
// cliente elegido, lo decide quien lo usa con `onError`.
export function useCreateManualAppointment(): UseCreateManualAppointmentResult {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: createManualAppointment,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['company-appointments'] })
      queryClient.invalidateQueries({ queryKey: ['company-availability'] })
    },
    onError: (error) => {
      if (!(error instanceof ApiRequestError)) {
        return
      }
      if (REJECTED_SLOT_CODES.includes(error.code)) {
        queryClient.invalidateQueries({ queryKey: ['company-availability'] })
      }
      if (error.code === 'SERVICE_NOT_FOUND') {
        queryClient.invalidateQueries({ queryKey: ['services'] })
      }
    },
  })

  return {
    create: (body, options) =>
      mutation.mutate(body, {
        onSuccess: options?.onSuccess,
        onError: (error) =>
          options?.onError?.(
            error instanceof ApiRequestError ? error.code : null,
          ),
      }),
    error: mutation.error ? toErrorMessage(mutation.error) : null,
    errorCode:
      mutation.error instanceof ApiRequestError ? mutation.error.code : null,
    isLoading: mutation.isPending,
    reset: mutation.reset,
  }
}
