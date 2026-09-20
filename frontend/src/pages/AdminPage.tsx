import { useCallback, useEffect, useState } from 'react'
import { usersApi } from '../api/users'
import { useStages } from '../hooks/useStages'
import { useCohorts } from '../hooks/useCohorts'
import { Loader } from '../components/Loader'
import { ErrorState } from '../components/ErrorState'
import { apiErrorMessage } from '../hooks/useToast'
import { StageSettings } from './admin/StageSettings'
import { CohortSettings } from './admin/CohortSettings'
import { UserManagement } from './admin/UserManagement'
import { pluralRu } from '../utils/plural'
import type { UserDto } from '../types'

function timeToOfferLabel(stages: { position: number; normDays: number | null }[]): string {
  const timeToOffer = [...stages]
    .sort((a, b) => a.position - b.position)
    .slice(0, 8)
    .reduce((sum, s) => sum + (s.normDays ?? 0), 0)
  const months = Math.floor(timeToOffer / 30)
  const remDays = timeToOffer % 30
  const parts: string[] = []
  if (months > 0) parts.push(`${months} ${pluralRu(months, ['месяц', 'месяца', 'месяцев'])}`)
  if (remDays > 0 || months === 0) parts.push(`${remDays} ${pluralRu(remDays, ['день', 'дня', 'дней'])}`)
  return `${timeToOffer} дн. (${parts.join(' и ')})`
}

export function AdminPage() {
  const { stages, loading: stagesLoading, error: stagesError, refetch: refetchStages } = useStages()
  const { cohorts, loading: cohortsLoading, error: cohortsError, refetch: refetchCohorts } = useCohorts()
  const [users, setUsers] = useState<UserDto[]>([])
  const [usersLoading, setUsersLoading] = useState(true)
  const [usersError, setUsersError] = useState<string | null>(null)

  const refetchUsers = useCallback(() => {
    setUsersLoading(true)
    return usersApi
      .list()
      .then(setUsers)
      .catch((e) => setUsersError(apiErrorMessage(e)))
      .finally(() => setUsersLoading(false))
  }, [])

  useEffect(() => {
    refetchUsers()
  }, [refetchUsers])

  if (stagesLoading || cohortsLoading || usersLoading) return <Loader />
  if (stagesError || cohortsError || usersError) {
    return <ErrorState message={stagesError ?? cohortsError ?? usersError ?? 'Ошибка'} />
  }

  return (
    <div className="flex flex-col gap-8">
      <div className="card px-5 py-4 min-w-[200px] self-start">
        <div className="font-display font-bold text-2xl">{timeToOfferLabel(stages)}</div>
        <div className="text-xs text-ink-600 mt-0.5">Ожидаемое время с нуля до оффера (этапы 1–8)</div>
      </div>
      <StageSettings stages={stages} onChanged={refetchStages} />
      <CohortSettings cohorts={cohorts} onChanged={refetchCohorts} />
      <UserManagement users={users} onChanged={refetchUsers} />
    </div>
  )
}
