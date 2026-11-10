import type { Company } from './Company'

// Respuesta de GET y PUT /api/company/branding. El slug es de solo lectura: sirve para armar el link a la
// página pública, porque la sesión del admin no lo trae. La zona horaria también es de solo lectura: el panel muestra
// los turnos en la hora de la empresa.
export type CompanyBranding = Pick<
  Company,
  'slug' | 'logoUrl' | 'primaryColor' | 'timezone'
>

// Cuerpo de PUT /api/company/branding. Si un campo va en null, el backend lo borra.
export type CompanyBrandingData = Pick<CompanyBranding, 'logoUrl' | 'primaryColor'>
