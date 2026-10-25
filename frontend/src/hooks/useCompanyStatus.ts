import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toErrorMessage } from '../api/client'
import { deactivateCompany, reactivateCompany } from '../api/companies'
import type { Company } from '../types/Company'

interface UseCompanyStatusResult {
  toggleStatus: (company: Company) => void
  error: string | null
  isLoading: boolean
}

function deactivationMessage(name: string): string {
  return `¿Desactivar "${name}"? Se cancelan sus turnos pendientes y se avisa a cada cliente. Su admin no va a poder iniciar sesión.`
}

// Reactiva una empresa inactiva. Una activa se desactiva recién después de que el superadmin confirme
// (US-04.4 y US-04.5). Devuelve null si cancela.
async function toggle(company: Company): Promise<Company | null> {
  if (company.status === 'INACTIVE') {
    return reactivateCompany(company.id)
  }
  if (!window.confirm(deactivationMessage(company.name))) {
    return null
  }
  return deactivateCompany(company.id)
}

export function useCompanyStatus(): UseCompanyStatusResult {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: toggle,
    onSuccess: (result) => {
      if (result) {
        return queryClient.invalidateQueries({ queryKey: ['companies'] })
      }
    },
  })

  return {
    toggleStatus: mutation.mutate,
    error: mutation.error ? toErrorMessage(mutation.error) : null,
    isLoading: mutation.isPending,
  }
}
