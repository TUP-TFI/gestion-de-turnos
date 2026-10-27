import { useQuery } from '@tanstack/react-query'
import { getBranding } from '../api/branding'
import { toErrorMessage } from '../api/client'
import type { CompanyBranding } from '../types/CompanyBranding'

interface UseBrandingResult {
  branding: CompanyBranding | null
  error: string | null
  isLoading: boolean
}

export function useBranding(): UseBrandingResult {
  const query = useQuery({
    queryKey: ['branding'],
    queryFn: getBranding,
  })

  return {
    branding: query.data ?? null,
    error: query.error ? toErrorMessage(query.error) : null,
    isLoading: query.isPending,
  }
}
