import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { toErrorMessage } from '../api/client'
import { listCompanyAppointments } from '../api/companyAppointments'
import type { AppointmentFilters } from '../api/companyAppointments'
import type { AdminAppointment } from '../types/Appointment'
import type { PageResponse } from '../types/Page'

interface UseCompanyAppointmentsResult {
  appointments: PageResponse<AdminAppointment> | null
  error: string | null
  isLoading: boolean
  // Hay una consulta en curso, también cuando ya se ve la tabla anterior (cambio de página o de filtro).
  isFetching: boolean
}

export function useCompanyAppointments(
  filters: AppointmentFilters,
  page: number,
): UseCompanyAppointmentsResult {
  const query = useQuery({
    queryKey: ['company-appointments', 'list', filters, page],
    queryFn: () => listCompanyAppointments(filters, page),
    // Al cambiar de página o de filtro se sigue viendo la tabla anterior hasta que llega la nueva, sin parpadeo.
    placeholderData: keepPreviousData,
  })

  return {
    appointments: query.data ?? null,
    error: query.error ? toErrorMessage(query.error) : null,
    isLoading: query.isPending,
    isFetching: query.isFetching,
  }
}
