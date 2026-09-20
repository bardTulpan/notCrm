import { useCallback, useEffect, useState } from 'react'
import { stagesApi } from '../api/stages'
import type { StageDto } from '../types'

export function useStages() {
  const [stages, setStages] = useState<StageDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refetch = useCallback(() => {
    setLoading(true)
    setError(null)
    return stagesApi
      .list()
      .then(setStages)
      .catch((e) => setError(e?.message ?? 'Не удалось загрузить этапы'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    refetch()
  }, [refetch])

  return { stages, loading, error, refetch }
}
