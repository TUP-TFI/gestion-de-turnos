import { useEffect, useRef, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiRequestError, toErrorMessage } from '../api/client'
import { cancelCompanyAppointment } from '../api/companyAppointments'
import type { AdminAppointment } from '../types/Appointment'

// El turno ya no se puede cancelar o no existe: no sirve reintentar, hay que mostrar el motivo y recargar la lista.
const CLOSING_CODES = ['APPOINTMENT_NOT_FOUND', 'APPOINTMENT_NOT_PENDING']

// Cada aviso lleva un id, para que la página lo muestre con `key`: un aviso nuevo es un componente nuevo, y se enfoca
// solo al montarse (DismissibleNotice).
let nextNoticeId = 0

export interface CancelNotice {
  id: number
  kind: 'success' | 'info'
  text: string
}

interface UseCancelCompanyAppointmentResult {
  // El turno que se está confirmando en el modal, o null si el modal está cerrado.
  target: AdminAppointment | null
  // Mensaje para mostrar en la página, con el modal ya cerrado.
  notice: CancelNotice | null
  dismissNotice: () => void
  start: (appointment: AdminAppointment) => void
  confirm: (reason: string) => void
  close: () => void
  // Error para mostrar en el modal, por ejemplo COMPANY_INACTIVE o una falla de conexión.
  error: string | null
  isLoading: boolean
}

// Cancelar un turno desde la agenda o el dashboard del admin (US-09.3). Con éxito o con un 404 o 409 se invalida
// ['company-appointments'], que cubre las dos pantallas.
//
// También lleva el foco, para que no se pierda al cerrar el modal. Con "Volver" o Esc vuelve al botón que lo abrió. Con
// un aviso nuevo (éxito, 404 o 409) va al aviso, porque la fila del botón puede haber desaparecido.
export function useCancelCompanyAppointment(): UseCancelCompanyAppointmentResult {
  const queryClient = useQueryClient()
  const [target, setTarget] = useState<AdminAppointment | null>(null)
  const [notice, setNotice] = useState<CancelNotice | null>(null)
  // El botón que tenía el foco al abrir el modal, y cuántas veces se cerró el modal sin un aviso nuevo. El botón va en
  // estado y no en un ref: las funciones que devuelve el hook no pueden tocar refs, porque el lint (react-hooks/refs)
  // trata entonces todo el resultado como un ref y rechaza leerlo al renderizar.
  const [trigger, setTrigger] = useState<HTMLElement | null>(null)
  const [closeCount, setCloseCount] = useState(0)
  // Último cierre ya atendido. El efecto de abajo también depende de `trigger`, que cambia al abrir el modal: sin esto
  // enfocaría el botón en cada apertura.
  const handledCloseRef = useRef(0)

  // Corre con el modal ya desmontado. Si el botón ya no está en el DOM no hay adónde volver: un aviso nuevo se enfoca
  // solo.
  useEffect(() => {
    if (closeCount === handledCloseRef.current) {
      return
    }
    handledCloseRef.current = closeCount
    if (trigger?.isConnected) {
      trigger.focus()
    }
  }, [closeCount, trigger])

  function refreshAppointments() {
    queryClient.invalidateQueries({ queryKey: ['company-appointments'] })
  }

  const mutation = useMutation({
    mutationFn: (variables: { appointment: AdminAppointment; reason: string }) =>
      cancelCompanyAppointment(variables.appointment.id, variables.reason),
    onSuccess: (_data, variables) => {
      refreshAppointments()
      setTarget(null)
      setNotice({
        id: ++nextNoticeId,
        kind: 'success',
        text: `Cancelaste el turno de ${variables.appointment.client.name}.`,
      })
    },
    onError: (error) => {
      if (
        !(error instanceof ApiRequestError) ||
        !CLOSING_CODES.includes(error.code)
      ) {
        return
      }
      refreshAppointments()
      setTarget(null)
      setNotice({
        id: ++nextNoticeId,
        kind: 'info',
        text: toErrorMessage(error),
      })
    },
  })

  return {
    target,
    notice,
    dismissNotice: () => setNotice(null),
    start: (appointment) => {
      // Sin foco en un botón, por ejemplo en Safari con el mouse, activeElement es el body: no hay adónde volver.
      const active = document.activeElement
      setTrigger(
        active instanceof HTMLElement && active !== document.body
          ? active
          : null,
      )
      setNotice(null)
      mutation.reset()
      setTarget(appointment)
    },
    confirm: (reason) => {
      if (target) {
        mutation.mutate({ appointment: target, reason })
      }
    },
    close: () => {
      mutation.reset()
      setTarget(null)
      setCloseCount((count) => count + 1)
    },
    error: mutation.error ? toErrorMessage(mutation.error) : null,
    isLoading: mutation.isPending,
  }
}
