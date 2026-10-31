import { AuthCard } from './AuthCard'

// Lo que ven la página pública, el login y el registro del cliente cuando el slug de la URL no es de ninguna empresa.
export function CompanyNotFoundCard() {
  return (
    <AuthCard
      title="La empresa no existe"
      subtitle="Revisá que la dirección esté bien escrita."
    >
      {null}
    </AuthCard>
  )
}
