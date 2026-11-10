import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiRequestError, toErrorMessage } from '../api/client'
import {
  cancelAppointment,
  rescheduleAppointment,
} from '../api/clientAppointments'
import type { ClientAppointment } from '../types/Appointment'

// El turno ya no se puede cancelar o reprogramar, o no existe: no sirve reintentar, hay que mostrar el motivo y
// recargar la lista.
const CANCEL_CLOSING_CODES = [
  'APPOINTMENT_NOT_FOUND',
  'APPOINTMENT_NOT_PENDING',
  'CANCELLATION_DEADLINE_PASSED',
]

const RESCHEDULE_CLOSING_CODES = [
  'APPOINTMENT_NOT_FOUND',
  'APPOINTMENT_NOT_PENDING',
  'RESCHEDULE_DEADLINE_PASSED',
  'RESCHEDULE_LIMIT_REACHED',
]

// El horario elegido ya no sirve: se queda abierto el flujo para elegir otro.
const REJECTED_SLOT_CODES = [
  'SLOT_UNAVAILABLE',
  'BOOKING_TOO_SOON',
  'BOOKING_TOO_FAR',
]

function hasCode(error: Error, codes: string[]): boolean {
  return error instanceof ApiRequestError && codes.includes(error.code)
}

export interface AppointmentNotice {
  kind: 'success' | 'info'
  text: string
}

interface UseAppointmentActionsResult {
  // Mensaje para mostrar en la página, con los modales ya cerrados.
  notice: AppointmentNotice | null
  cancel: {
    // El turno que se está confirmando en el modal, o null si el modal está cerrado.
    target: ClientAppointment | null
    start: (appointment: ClientAppointment) => void
    confirm: () => void
    close: () => void
    error: string | null
    isLoading: boolean
  }
  reschedule: {
    target: ClientAppointment | null
    start: (appointment: ClientAppointment) => void
    confirm: (startDateTime: string) => void
    close: () => void
    // Un error de los horarios que cierra el flujo, por ejemplo si pasó el plazo mientras se elegía.
    handleAvailabilityError: (error: Error) => void
    // Error para mostrar en el modal.
    error: string | null
    // El horario elegido se rechazó: hay que elegir otro.
    isSlotRejected: boolean
    resetError: () => void
    isLoading: boolean
  }
}

// Cancelar y reprogramar un turno desde "Mis turnos" (US-08.1 y US-08.2). Con cualquier respuesta que cierra el
// flujo se recarga la lista, porque lo que mostraba ya no es verdad.
export function useAppointmentActions(
  slug: string,
): UseAppointmentActionsResult {
  const queryClient = useQueryClient()
  const [cancelTarget, setCancelTarget] = useState<ClientAppointment | null>(
    null,
  )
  const [rescheduleTarget, setRescheduleTarget] =
    useState<ClientAppointment | null>(null)
  const [notice, setNotice] = useState<AppointmentNotice | null>(null)

  function refreshAppointments() {
    queryClient.invalidateQueries({ queryKey: ['client-appointments', slug] })
  }

  function closeRescheduleWith(error: Error) {
    refreshAppointments()
    setRescheduleTarget(null)
    setNotice({ kind: 'info', text: toErrorMessage(error) })
  }

  const cancelMutation = useMutation({
    mutationFn: cancelAppointment,
    onSuccess: () => {
      refreshAppointments()
      setCancelTarget(null)
      setNotice({ kind: 'success', text: 'Cancelaste tu turno.' })
    },
    onError: (error) => {
      if (!hasCode(error, CANCEL_CLOSING_CODES)) {
        return
      }
      refreshAppointments()
      setCancelTarget(null)
      setNotice({ kind: 'info', text: toErrorMessage(error) })
    },
  })

  const rescheduleMutation = useMutation({
    mutationFn: (variables: { id: string; startDateTime: string }) =>
      rescheduleAppointment(variables.id, variables.startDateTime),
    onSuccess: () => {
      refreshAppointments()
      queryClient.invalidateQueries({ queryKey: ['availability', slug] })
      queryClient.invalidateQueries({
        queryKey: ['reschedule-availability', slug],
      })
      setRescheduleTarget(null)
      setNotice({ kind: 'success', text: 'Reprogramaste tu turno.' })
    },
    onError: (error, variables) => {
      if (hasCode(error, REJECTED_SLOT_CODES)) {
        queryClient.invalidateQueries({
          queryKey: ['reschedule-availability', slug, variables.id],
        })
        return
      }
      if (hasCode(error, RESCHEDULE_CLOSING_CODES)) {
        closeRescheduleWith(error)
      }
    },
  })

  return {
    notice,
    cancel: {
      target: cancelTarget,
      start: (appointment) => {
        setNotice(null)
        cancelMutation.reset()
        setCancelTarget(appointment)
      },
      confirm: () => {
        if (cancelTarget) {
          cancelMutation.mutate(cancelTarget.id)
        }
      },
      close: () => {
        cancelMutation.reset()
        setCancelTarget(null)
      },
      error: cancelMutation.error ? toErrorMessage(cancelMutation.error) : null,
      isLoading: cancelMutation.isPending,
    },
    reschedule: {
      target: rescheduleTarget,
      start: (appointment) => {
        setNotice(null)
        rescheduleMutation.reset()
        setRescheduleTarget(appointment)
      },
      confirm: (startDateTime) => {
        if (rescheduleTarget) {
          rescheduleMutation.mutate({ id: rescheduleTarget.id, startDateTime })
        }
      },
      close: () => {
        rescheduleMutation.reset()
        setRescheduleTarget(null)
      },
      handleAvailabilityError: (error) => {
        // Mismos 404 y 409 que el cancelar, según el contrato. Cualquier otro error se muestra en el selector.
        if (hasCode(error, CANCEL_CLOSING_CODES)) {
          closeRescheduleWith(error)
        }
      },
      error: rescheduleMutation.error
        ? toErrorMessage(rescheduleMutation.error)
        : null,
      isSlotRejected:
        rescheduleMutation.error !== null &&
        hasCode(rescheduleMutation.error, REJECTED_SLOT_CODES),
      resetError: () => rescheduleMutation.reset(),
      isLoading: rescheduleMutation.isPending,
    },
  }
}
