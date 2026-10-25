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

function changeStatus(company: Company): Promise<Company> {
  return company.status === 'INACTIVE'
    ? reactivateCompany(company.id)
    : deactivateCompany(company.id)
}

export function useCompanyStatus(): UseCompanyStatusResult {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: changeStatus,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['companies'] }),
  })

  // Reactiva una empresa inactiva. Una activa se desactiva recién después de que el superadmin confirme
  // (US-04.4 y US-04.5): mientras el aviso está abierto no hay mutación en curso.
  function toggleStatus(company: Company) {
    mutation.reset()

    if (company.status === 'ACTIVE' && !window.confirm(deactivationMessage(company.name))) {
      return
    }
    mutation.mutate(company)
  }

  return {
    toggleStatus,
    error: mutation.error ? toErrorMessage(mutation.error) : null,
    isLoading: mutation.isPending,
  }
}
