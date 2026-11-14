import { useEffect } from 'react'
import { useSearchParams } from 'react-router'
import type { PageResponse } from '../types/Page'

// Páginas de la URL (`?page=`) contra lo que devuelve el back. Si la URL pide una página que ya no existe, por
// ejemplo porque se borraron elementos o se editó a mano, la reemplaza por la última, o por la primera si no queda
// ninguno. Devuelve `true` mientras la respuesta es de una página que no existe: con una respuesta así no se muestra
// la tabla, ni el paginado, ni el estado vacío.
export function usePageInRange(
  result: PageResponse<unknown> | null,
  page: number,
): boolean {
  const [params, setParams] = useSearchParams()

  const isOutOfRange =
    result !== null && result.page > 0 && result.page >= result.totalPages
  const lastPage = result ? result.totalPages - 1 : 0
  // Solo se corrige la URL si la respuesta es de la página que la URL pide. Después de corregirla, keepPreviousData
  // sigue mostrando la respuesta vieja (`?page=99`) hasta que llega la nueva. Cada setParams crea una ubicación nueva
  // aunque la URL sea la misma, y eso cambia `params` y vuelve a correr el efecto: sin esta condición se repetiría la
  // navegación durante toda la consulta.
  const shouldRedirect = isOutOfRange && result?.page === page

  // Se reemplaza la URL, sin sumar una entrada al historial.
  useEffect(() => {
    if (!shouldRedirect) {
      return
    }
    const next = new URLSearchParams(params)
    if (lastPage > 0) {
      next.set('page', String(lastPage))
    } else {
      next.delete('page')
    }
    setParams(next, { replace: true })
  }, [shouldRedirect, lastPage, params, setParams])

  return isOutOfRange
}
