import { useQuery } from '@tanstack/react-query'
import { toErrorMessage } from '../api/client'
import { listServices } from '../api/services'
import type { Service } from '../types/Service'

interface UseServicesResult {
  services: Service[]
  error: string | null
  isLoading: boolean
}

export function useServices(): UseServicesResult {
  const query = useQuery({
    queryKey: ['services'],
    queryFn: listServices,
  })

  return {
    services: query.data ?? [],
    error: query.error ? toErrorMessage(query.error) : null,
    isLoading: query.isPending,
  }
}
