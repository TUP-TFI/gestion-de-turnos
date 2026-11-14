import { useEffect, useState } from 'react'

// Devuelve `value` recién cuando pasaron `delayMs` sin que cambie. Cada cambio reinicia la espera.
export function useDebouncedValue<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value)

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs)
    return () => clearTimeout(timer)
  }, [value, delayMs])

  return debounced
}
