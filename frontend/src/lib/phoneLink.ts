// Href `tel:` de un teléfono: sin espacios, guiones ni paréntesis, y con el `+` si lo trae. El texto que se muestra no
// cambia: se ve tal como se guardó.
export function telHref(phone: string): string {
  return `tel:${phone.replace(/[^\d+]/g, '')}`
}
