import type { CompanyBrandingData } from '../types/CompanyBranding'
import { toNullable } from './companyForm'

// Arma el body del PUT con lo que cargó el admin. Un campo vacío va en null, así el backend lo borra.
export function readBrandingData(data: FormData): CompanyBrandingData {
  return {
    logoUrl: toNullable(data.get('logoUrl')),
    primaryColor: toNullable(data.get('primaryColor')),
  }
}
