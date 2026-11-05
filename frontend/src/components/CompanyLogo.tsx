import { useState } from 'react'

// Logo de la empresa en la banda de la página pública y en la vista previa de "Marca". Si la imagen no carga, se
// oculta en lugar de mostrar el ícono roto. Quien lo usa le pone una key con la URL, para que un logo nuevo vuelva a
// intentar. El alt va vacío porque el nombre de la empresa está al lado.
export function CompanyLogo({ url }: { url: string }) {
  const [failed, setFailed] = useState(false)

  if (failed) {
    return null
  }

  return (
    <img
      src={url}
      alt=""
      onError={() => setFailed(true)}
      className="h-10 w-10 shrink-0 rounded-lg bg-white object-contain"
    />
  )
}
