import { useCallback, useEffect, useState } from 'react'
import { cohortsApi } from '../api/cohorts'
import type { CohortDto } from '../types'

export function useCohorts() {
  const [cohorts, setCohorts] = useState<CohortDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refetch = useCallback(() => {
    setLoading(true)
    setError(null)
    return cohortsApi
      .list()
      .then(setCohorts)
      .catch((e) => setError(e?.message ?? 'Не удалось загрузить когорты'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    refetch()
  }, [refetch])

  return { cohorts, loading, error, refetch }
}
