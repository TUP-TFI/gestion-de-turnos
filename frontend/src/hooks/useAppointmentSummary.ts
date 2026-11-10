import { useQuery } from '@tanstack/react-query'
import { toErrorMessage } from '../api/client'
import { getAppointmentSummary } from '../api/companyAppointments'
import type { AppointmentSummary } from '../types/Appointment'

interface UseAppointmentSummaryResult {
  summary: AppointmentSummary | null
  error: string | null
  isLoading: boolean
}

export function useAppointmentSummary(): UseAppointmentSummaryResult {
  const query = useQuery({
    // Prefijo común con la agenda: una sola invalidación de ['company-appointments'] las cubre.
    queryKey: ['company-appointments', 'summary'],
    queryFn: getAppointmentSummary,
  })

  return {
    summary: query.data ?? null,
    error: query.error ? toErrorMessage(query.error) : null,
    isLoading: query.isPending,
  }
}
