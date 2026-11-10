// Menos dígitos que esto no puede ser un número de teléfono con código de país.
const MIN_PHONE_DIGITS = 8

// Link de Click to Chat: abre WhatsApp con el texto cargado, y una persona toca enviar. Deja solo los dígitos del
// teléfono y quita un `00` inicial (prefijo internacional). No adivina el código de país: un número local abre otro
// chat o da error, y el link no lo puede detectar. Devuelve null si el teléfono no sirve.
export function buildWhatsAppUrl(
  phone: string | null | undefined,
  text: string,
): string | null {
  const digits = (phone ?? '').replace(/\D/g, '').replace(/^00/, '')
  if (digits.length < MIN_PHONE_DIGITS) {
    return null
  }
  return `https://wa.me/${digits}?text=${encodeURIComponent(text)}`
}
