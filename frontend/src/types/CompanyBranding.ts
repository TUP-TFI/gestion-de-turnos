import type { Company } from './Company'

// Respuesta de GET y PUT /api/company/branding. El slug es de solo lectura: sirve para armar el link a la
// página pública, porque la sesión del admin no lo trae.
export type CompanyBranding = Pick<Company, 'slug' | 'logoUrl' | 'primaryColor'>

// Cuerpo de PUT /api/company/branding. Si un campo va en null, el backend lo borra.
export type CompanyBrandingData = Pick<CompanyBranding, 'logoUrl' | 'primaryColor'>
