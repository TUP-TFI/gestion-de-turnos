import { useEffect, useRef } from 'react'
import type { ReactNode } from 'react'
import { NOTICE_CLASSES, SUCCESS_CLASSES } from '../lib/formStyles'

interface DismissibleNoticeProps {
  kind: 'success' | 'info'
  children: ReactNode
  // Links que siguen al texto, como "Ver en la agenda" o "Avisar por WhatsApp". Van en una fila aparte, debajo.
  actions?: ReactNode
  onDismiss: () => void
}

// Aviso de la página con un botón para cerrarlo. Éxito en verde, y en ámbar lo que el admin tiene que saber sin que
// haya fallado su acción. Se enfoca al montarse: tras cerrar un modal, la fila que lo abrió puede haber desaparecido.
// Quien lo usa le pone una `key` distinta por aviso. Lleva tabIndex -1 para recibir el foco por código, sin entrar en
// el orden del Tab.
export function DismissibleNotice({
  kind,
  children,
  actions,
  onDismiss,
}: DismissibleNoticeProps) {
  const noticeRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    noticeRef.current?.focus()
  }, [])

  // focus-visible y no focus: el foco llega por código, y con el mouse el anillo lo haría ver distinto de los otros
  // avisos. Con teclado sí se ve.
  return (
    <div
      ref={noticeRef}
      role="status"
      tabIndex={-1}
      className={`flex items-start justify-between gap-3 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-light ${kind === 'success' ? SUCCESS_CLASSES : NOTICE_CLASSES}`}
    >
      {/* gap y no space-y: con la fila de acciones oculta, space-y igual le deja margen al texto. */}
      <div className="flex flex-col gap-2">
        <p>{children}</p>
        {/* empty:hidden: una acción puede no mostrar nada, como WhatsApp sin mensaje, y la fila no deja un hueco. */}
        {actions && (
          <div className="flex flex-wrap gap-x-4 gap-y-1 empty:hidden">
            {actions}
          </div>
        )}
      </div>
      <button
        type="button"
        aria-label="Cerrar aviso"
        onClick={onDismiss}
        className="-my-0.5 cursor-pointer rounded px-1 text-lg leading-none focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-light"
      >
        ×
      </button>
    </div>
  )
}
