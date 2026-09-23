import { useEffect, useState } from 'react'
import { getPing } from '../api/ping'

interface UsePingResult {
  data: string | null
  error: string | null
  isLoading: boolean
}

export function usePing(): UsePingResult {
  const [data, setData] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    getPing()
      .then(setData)
      .catch((err: Error) => setError(err.message))
      .finally(() => setIsLoading(false))
  }, [])

  return { data, error, isLoading }
}
