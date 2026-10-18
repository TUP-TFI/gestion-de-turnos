import type { CompanyCategory, CompanyData } from '../types/Company'

export function toNullable(value: FormDataEntryValue | null): string | null {
  const text = typeof value === 'string' ? value.trim() : ''
  return text === '' ? null : text
}

export function toRequired(value: FormDataEntryValue | null): string {
  return typeof value === 'string' ? value.trim() : ''
}

// Arma los datos de la empresa desde el form de CompanyFields.
export function readCompanyData(data: FormData): CompanyData {
  return {
    name: toRequired(data.get('name')),
    category: toRequired(data.get('category')) as CompanyCategory,
    timezone: toRequired(data.get('timezone')),
    description: toNullable(data.get('description')),
    address: toNullable(data.get('address')),
    phone: toNullable(data.get('phone')),
    contactEmail: toNullable(data.get('contactEmail')),
    logoUrl: toNullable(data.get('logoUrl')),
  }
}
