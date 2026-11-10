import { STATUS_CLASSES, STATUS_LABELS } from '../lib/appointmentLabels'
import { BADGE_CLASSES, BADGE_VARIANT_CLASSES } from '../lib/formStyles'
import type { AdminAppointment } from '../types/Appointment'

interface AppointmentStatusBadgeProps {
  appointment: Pick<AdminAppointment, 'status' | 'rescheduleCount'>
}

// Estado del turno, más "Reprogramado" si ya se movió de horario.
export function AppointmentStatusBadge({
  appointment,
}: AppointmentStatusBadgeProps) {
  return (
    <div className="flex flex-wrap gap-2">
      <span className={`${BADGE_CLASSES} ${STATUS_CLASSES[appointment.status]}`}>
        {STATUS_LABELS[appointment.status]}
      </span>
      {appointment.rescheduleCount > 0 && (
        <span className={`${BADGE_CLASSES} ${BADGE_VARIANT_CLASSES.neutral}`}>
          Reprogramado
        </span>
      )}
    </div>
  )
}
