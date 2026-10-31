import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useLocation, useNavigate } from 'react-router'
import { ApiRequestError, toErrorMessage } from '../api/client'
import { createAppointment } from '../api/clientAppointments'
import { readBookingParams, toBookingSearch } from '../lib/bookingParams'
import type { PublicService, SlotSelection } from '../types/PublicCompany'
import { useClientAuth } from './useClientAuth'

// Con estos errores, confirmar de nuevo no sirve: hay que elegir otro horario.
const REJECTED_SLOT_CODES = [
  'SLOT_UNAVAILABLE',
  'BOOKING_TOO_SOON',
  'BOOKING_TOO_FAR',
  'SERVICE_NOT_FOUND',
]

function isRejectedSlot(error: Error): boolean {
  return (
    error instanceof ApiRequestError && REJECTED_SLOT_CODES.includes(error.code)
  )
}

interface UseBookingFlowResult {
  // El turno que se está confirmando en el modal, o null si el modal está cerrado.
  selection: SlotSelection | null
  startBooking: (selection: SlotSelection) => void
  confirmBooking: () => void
  cancelBooking: () => void
  // Error para mostrar en el modal.
  error: string | null
  // Motivo por el que se rechazó el horario, para mostrar en la página con el modal ya cerrado.
  rejection: string | null
  isBooked: boolean
  isLoading: boolean
}

// Lleva al cliente desde que elige un horario hasta que el turno queda reservado (US-07.1, 07.2, 07.3 y 07.7).
export function useBookingFlow(
  slug: string,
  services: PublicService[],
): UseBookingFlowResult {
  const navigate = useNavigate()
  const location = useLocation()
  const queryClient = useQueryClient()
  const { session } = useClientAuth(slug)
  const [picked, setPicked] = useState<SlotSelection | null>(null)
  const [rejection, setRejection] = useState<string | null>(null)
  const [isBooked, setIsBooked] = useState(false)

  // Al volver del login o del registro, la selección llega en la URL. Si el servicio ya no está, se ignora.
  const returning = session ? readBookingParams(location.search) : null
  const returningService = services.find(
    (service) => service.id === returning?.serviceId,
  )
  const selection =
    picked ??
    (returning && returningService
      ? { service: returningService, startDateTime: returning.start }
      : null)

  function closeModal() {
    setPicked(null)
    // Sin esto, el modal se volvería a abrir con los datos de la URL.
    if (returning) {
      navigate(location.pathname, { replace: true })
    }
  }

  function refreshAvailability() {
    // Sin serviceId ni fecha, abarca todos los horarios de esta empresa.
    queryClient.invalidateQueries({ queryKey: ['availability', slug] })
  }

  const mutation = useMutation({
    mutationFn: createAppointment,
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['client-appointments', slug],
      })
      refreshAvailability()
      closeModal()
      setIsBooked(true)
    },
    onError: (error) => {
      if (!isRejectedSlot(error)) {
        return
      }
      refreshAvailability()
      // Con SERVICE_NOT_FOUND, el servicio tiene que desaparecer de la lista.
      queryClient.invalidateQueries({ queryKey: ['public-company', slug] })
      closeModal()
      setRejection(toErrorMessage(error))
    },
  })

  function startBooking(newSelection: SlotSelection) {
    setRejection(null)
    setIsBooked(false)

    if (!session) {
      const search = toBookingSearch({
        serviceId: newSelection.service.id,
        start: newSelection.startDateTime,
      })
      navigate(`/${slug}/login${search}`)
      return
    }

    mutation.reset()
    setPicked(newSelection)
  }

  function confirmBooking() {
    if (selection) {
      mutation.mutate({
        serviceId: selection.service.id,
        startDateTime: selection.startDateTime,
      })
    }
  }

  function cancelBooking() {
    mutation.reset()
    closeModal()
  }

  return {
    selection,
    startBooking,
    confirmBooking,
    cancelBooking,
    error: mutation.error ? toErrorMessage(mutation.error) : null,
    rejection,
    isBooked,
    isLoading: mutation.isPending,
  }
}
