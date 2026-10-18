export const DEFAULT_TIMEZONE = 'America/Argentina/Cordoba'

const SUPPORTED_TIMEZONES = Intl.supportedValuesOf('timeZone')

// Chrome no trae America/Argentina/Cordoba en Intl.supportedValuesOf, pero el backend sí la acepta.
export const TIMEZONES = SUPPORTED_TIMEZONES.includes(DEFAULT_TIMEZONE)
  ? SUPPORTED_TIMEZONES
  : [DEFAULT_TIMEZONE, ...SUPPORTED_TIMEZONES]
