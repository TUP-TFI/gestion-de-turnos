import { useEffect, useRef } from 'react'
import type { ReactNode } from 'react'
import { NOTICE_CLASSES, SUCCESS_CLASSES } from '../lib/formStyles'

interface DismissibleNoticeProps {
  kind: 'success' | 'info'
  children: ReactNode
  onDismiss: () => void
}

// Aviso de la página con un botón para cerrarlo. Éxito en verde, y en ámbar lo que el admin tiene que saber sin que
// haya fallado su acción. Se enfoca al montarse: tras cerrar un modal, la fila que lo abrió puede haber desaparecido.
// Quien lo usa le pone una `key` distinta por aviso. Lleva tabIndex -1 para recibir el foco por código, sin entrar en
// el orden del Tab.
export function DismissibleNotice({
  kind,
  children,
  onDismiss,
}: DismissibleNoticeProps) {
  const noticeRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    noticeRef.current?.focus()
  }, [])

  return (
    <div
      ref={noticeRef}
      role="status"
      tabIndex={-1}
      className={`flex items-start justify-between gap-3 focus:outline-none focus:ring-2 focus:ring-brand-light ${kind === 'success' ? SUCCESS_CLASSES : NOTICE_CLASSES}`}
    >
      <p>{children}</p>
      <button
        type="button"
        aria-label="Cerrar aviso"
        onClick={onDismiss}
        className="-my-0.5 cursor-pointer rounded px-1 text-lg leading-none focus:outline-none focus:ring-2 focus:ring-brand-light"
      >
        ×
      </button>
    </div>
  )
}
