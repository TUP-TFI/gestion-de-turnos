// Solo los dígitos, sin un `00` inicial (prefijo internacional).
function phoneDigits(phone: string | null | undefined): string {
  return (phone ?? '').replace(/\D/g, '').replace(/^00/, '')
}

// Menos dígitos que esto no puede ser un número de teléfono con código de país.
const MIN_PHONE_DIGITS = 8

// Link de Click to Chat: abre WhatsApp con el texto cargado, y una persona toca enviar. Deja solo los dígitos del
// teléfono y quita un `00` inicial (prefijo internacional). No adivina el código de país: un número local abre otro
// chat o da error, y el link no lo puede detectar. Devuelve null si el teléfono no sirve.
export function buildWhatsAppUrl(
  phone: string | null | undefined,
  text: string,
): string | null {
  const digits = phoneDigits(phone)
  if (digits.length < MIN_PHONE_DIGITS) {
    return null
  }
  return `https://wa.me/${digits}?text=${encodeURIComponent(text)}`
}

// Si el teléfono parece tener código de país: empieza con `+` o `00` y tiene los dígitos que pide el link. Es solo una
// pista para avisar antes de cargar: no valida que el número exista ni que el código sea el correcto.
export function looksInternational(phone: string): boolean {
  const text = phone.trim()
  const hasPrefix = text.startsWith('+') || text.startsWith('00')
  return hasPrefix && phoneDigits(text).length >= MIN_PHONE_DIGITS
}
