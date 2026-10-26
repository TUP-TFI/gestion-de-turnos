// Clases de Tailwind compartidas por las pantallas. Los valores salen de DESIGN.md.
export const LABEL_CLASSES = 'block text-sm font-medium text-ink'

export const INPUT_CLASSES =
  'mt-1 h-10 w-full rounded-lg border border-hairline bg-white px-3 text-ink focus:border-brand-light focus:outline-none focus:ring-2 focus:ring-brand-light/50'

// Igual que INPUT_CLASSES pero sin alto fijo, para los textarea.
export const TEXTAREA_CLASSES =
  'mt-1 w-full rounded-lg border border-hairline bg-white px-3 py-2 text-ink focus:border-brand-light focus:outline-none focus:ring-2 focus:ring-brand-light/50'

export const ERROR_CLASSES =
  'rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700'

export const SUCCESS_CLASSES =
  'rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700'

// Los botones no traen ancho: se agrega `w-full` donde haga falta. Sumar `w-auto` encima de `w-full` no
// sirve, porque en Tailwind gana la clase que aparece después en el CSS, no la última del className.
export const PRIMARY_BUTTON_CLASSES =
  'inline-flex h-10 cursor-pointer items-center justify-center rounded-lg bg-ink px-4 text-sm font-semibold text-white hover:bg-ink-active focus:outline-none focus:ring-2 focus:ring-brand-light disabled:cursor-not-allowed disabled:bg-hairline disabled:text-muted'

export const SECONDARY_BUTTON_CLASSES =
  'inline-flex h-10 cursor-pointer items-center justify-center rounded-lg border border-hairline bg-white px-4 text-sm font-semibold text-ink hover:bg-surface-card focus:outline-none focus:ring-2 focus:ring-brand-light disabled:cursor-not-allowed disabled:bg-hairline disabled:text-muted'

// Acciones de una fila de tabla: botón secundario compacto (32px). La variante de peligro es para
// las acciones destructivas, como desactivar una empresa.
export const ROW_BUTTON_CLASSES =
  'inline-flex h-8 cursor-pointer items-center justify-center rounded-lg border border-hairline bg-white px-3 text-sm font-semibold text-ink hover:bg-surface-card focus:outline-none focus:ring-2 focus:ring-brand-light disabled:cursor-not-allowed disabled:bg-hairline disabled:text-muted'

export const ROW_DANGER_BUTTON_CLASSES =
  'inline-flex h-8 cursor-pointer items-center justify-center rounded-lg border border-red-200 bg-white px-3 text-sm font-semibold text-red-700 hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-brand-light disabled:cursor-not-allowed disabled:border-hairline disabled:bg-hairline disabled:text-muted'

export const FIELDSET_CLASSES =
  'space-y-4 rounded-xl border border-hairline bg-white p-4 shadow-sm sm:p-6'

export const TABLE_WRAPPER_CLASSES =
  'overflow-x-auto rounded-xl border border-hairline bg-white'

export const TABLE_HEAD_CLASSES =
  'border-b border-hairline bg-surface-card text-ink'

export const TABLE_CELL_CLASSES = 'px-4 py-3'

export const BADGE_CLASSES =
  'inline-block rounded-full px-2.5 py-0.5 text-[13px] font-medium'

export const BADGE_VARIANT_CLASSES = {
  success: 'bg-emerald-50 text-emerald-700',
  neutral: 'bg-surface-card text-muted',
  warning: 'bg-amber-50 text-amber-700',
}

export const LINK_CLASSES = 'font-medium text-ink hover:underline'

export const PAGE_TITLE_CLASSES = 'text-[22px] font-semibold text-ink'

export const CARD_TITLE_CLASSES = 'text-lg font-semibold text-ink'

export const MUTED_TEXT_CLASSES = 'text-sm text-muted'
