import type { CSSProperties } from 'react'

// Sin color de la empresa, o con uno inválido, se usa el neutro `ink` de DESIGN.md.
const DEFAULT_ACCENT = '#111111'
const HEX_COLOR = /^#[0-9A-Fa-f]{6}$/
const MIN_TEXT_CONTRAST = 4.5

// Luminancia relativa de WCAG 2: 0 es negro y 1 es blanco.
function relativeLuminance(hexColor: string): number {
  const [red, green, blue] = [1, 3, 5].map((start) => {
    const channel = parseInt(hexColor.slice(start, start + 2), 16) / 255
    return channel <= 0.04045
      ? channel / 12.92
      : ((channel + 0.055) / 1.055) ** 2.4
  })
  return 0.2126 * red + 0.7152 * green + 0.0722 * blue
}

// Blanco si cumple el contraste AA de WCAG (4.5). Si no, el que tenga más contraste sobre ese fondo.
function textColorOn(hexColor: string): string {
  const luminance = relativeLuminance(hexColor)
  const contrastWithWhite = 1.05 / (luminance + 0.05)
  const contrastWithBlack = (luminance + 0.05) / 0.05
  if (contrastWithWhite >= MIN_TEXT_CONTRAST) {
    return '#ffffff'
  }
  return contrastWithWhite > contrastWithBlack ? '#ffffff' : '#000000'
}

// Variables CSS con el color de acento de la empresa y el texto que va encima. Las aplica el wrapper de la página.
export function companyThemeStyle(primaryColor: string | null): CSSProperties {
  const accent =
    primaryColor && HEX_COLOR.test(primaryColor) ? primaryColor : DEFAULT_ACCENT

  return {
    '--company-accent': accent,
    '--company-on-accent': textColorOn(accent),
  } as CSSProperties
}
