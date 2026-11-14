import type { User } from './User'

// Cliente de la empresa tal como lo ve su admin (ClientResponse del backend). Solo datos de contacto.
export type CompanyClient = Pick<
  User,
  'id' | 'firstName' | 'lastName' | 'email' | 'phone'
>
