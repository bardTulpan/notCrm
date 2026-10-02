import { useEffect, useState } from 'react'
import { statisticsApi } from '../api/statistics'
import { useAuth } from '../auth/useAuth'
import { useStages } from '../hooks/useStages'
import { Loader } from '../components/Loader'
import { ErrorState } from '../components/ErrorState'
import { apiErrorMessage } from '../hooks/useToast'
import { csvDateStamp, downloadCsv } from '../utils/csv'
import { formatDate } from '../utils/dates'
import { OverviewStats } from './stats/OverviewStats'
import { StageStatisticsTable } from './stats/StageStatisticsTable'
import { CuratorStatisticsTable } from './stats/CuratorStatisticsTable'
import { OverdueStudentsTable } from './stats/OverdueStudentsTable'
import { CohortFunnelTable } from './stats/CohortFunnelTable'
import { CuratorWorkloadTable } from './stats/CuratorWorkloadTable'
import { CohortDetailsModal } from './stats/CohortDetailsModal'
import type { CohortStats, CuratorStats, CuratorWorkload, OverdueStudent, StageStats, StatsOverview } from '../types'

export function StatisticsPage() {
  const { user } = useAuth()
  const { stages, loading: stagesLoading } = useStages()
  const [subTab, setSubTab] = useState<'main' | 'extra' | 'workload'>('main')
  const [overview, setOverview] = useState<StatsOverview | null>(null)
  const [stageStats, setStageStats] = useState<StageStats[]>([])
  const [curatorStats, setCuratorStats] = useState<CuratorStats[]>([])
  const [overdue, setOverdue] = useState<OverdueStudent[]>([])
  const [cohortStats, setCohortStats] = useState<CohortStats[]>([])
  const [workload, setWorkload] = useState<CuratorWorkload[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [openCohortId, setOpenCohortId] = useState<string | null>(null)

  useEffect(() => {
    setLoading(true)
    Promise.all([
      statisticsApi.overview(),
      statisticsApi.stages(),
      statisticsApi.curators(),
      statisticsApi.overdueStudents(),
      statisticsApi.cohorts(),
      user?.role === 'ADMIN' ? statisticsApi.curatorWorkload() : Promise.resolve([] as CuratorWorkload[]),
    ])
      .then(([ov, st, cur, od, coh, wl]) => {
        setOverview(ov)
        setStageStats(st)
        setCuratorStats(cur)
        setOverdue(od)
        setCohortStats(coh)
        setWorkload(wl)
      })
      .catch((e) => setError(apiErrorMessage(e)))
      .finally(() => setLoading(false))
  }, [user?.role])

  function exportFunnel() {
    downloadCsv(
      `воронка-когорт-${csvDateStamp()}.csv`,
      ['Когорта', 'Старт', 'Всего', ...stages.map((s, i) => `${i + 1}. ${s.name} (дошли)`), 'Идут по плану', 'Отстают'],
      cohortStats.map((c) => [
        c.name,
        formatDate(c.startDate),
        c.total,
        ...stages.map((_, i) => c.reachedCounts[i] ?? 0),
        c.onTrackCount,
        c.behindCount,
      ]),
    )
  }

  function exportWorkload() {
    downloadCsv(
      `нагрузка-кураторов-${csvDateStamp()}.csv`,
      ['Куратор', 'Учеников', 'Доля, %', 'В норме', 'Подходят к норме', 'Превысили норму', 'На паузе', 'Лиды в работе', 'Пинги просрочены', 'Новых за 30 дн.'],
      workload.map((w) => [w.name, w.students, w.sharePct, w.green, w.yellow, w.red, w.paused, w.activeLeads, w.overduePings, w.newLast30Days]),
    )
  }

  if (loading || stagesLoading) return <Loader />
  if (error || !overview) return <ErrorState message={error ?? 'Не удалось загрузить статистику'} />

  return (
    <div>
      <div className="flex items-center gap-2.5 mb-5">
        <button className={`chip ${subTab === 'main' ? 'active' : ''}`} onClick={() => setSubTab('main')}>
          Основная
        </button>
        <button className={`chip ${subTab === 'extra' ? 'active' : ''}`} onClick={() => setSubTab('extra')}>
          Дополнительная
        </button>
        {user?.role === 'ADMIN' && (
          <button className={`chip ${subTab === 'workload' ? 'active' : ''}`} onClick={() => setSubTab('workload')}>
            Нагрузка кураторов
          </button>
        )}
      </div>

      {subTab === 'main' && (
        <div>
          <div className="font-display font-semibold text-sm mb-2.5">Здоровье потока</div>
          <OverviewStats stats={overview} />

          <div className="font-display font-semibold text-sm mb-2.5">Статистика по этапам</div>
          <div className="mb-6">
            <StageStatisticsTable stages={stageStats} />
          </div>

          {user?.role === 'ADMIN' && (
            <>
              <div className="font-display font-semibold text-sm mb-2.5">По кураторам</div>
              <div className="mb-6">
                <CuratorStatisticsTable curators={curatorStats} />
              </div>
            </>
          )}

          <div className="font-display font-semibold text-sm mb-2.5">Превысили норму этапа</div>
          <OverdueStudentsTable students={overdue} />
        </div>
      )}

      {subTab === 'extra' && (
        <div>
          <div className="flex items-center justify-between mb-2.5">
            <div className="font-display font-semibold text-sm">Воронка доходимости по когортам</div>
            {cohortStats.length > 0 && (
              <button className="btn-ghost" onClick={exportFunnel}>
                Экспорт CSV
              </button>
            )}
          </div>
          {cohortStats.length === 0 ? (
            <div className="text-xs text-ink-400">Пока нет учеников ни в одной когорте.</div>
          ) : (
            <CohortFunnelTable cohorts={cohortStats} stages={stages} onOpenCohort={setOpenCohortId} />
          )}
        </div>
      )}

      {subTab === 'workload' && (
        <div>
          <div className="flex items-center justify-between mb-2.5 max-w-[980px]">
            <div className="font-display font-semibold text-sm">Нагрузка кураторов</div>
            {workload.length > 0 && (
              <button className="btn-ghost" onClick={exportWorkload}>
                Экспорт CSV
              </button>
            )}
          </div>
          <CuratorWorkloadTable rows={workload} />
        </div>
      )}

      {openCohortId && <CohortDetailsModal cohortId={openCohortId} stages={stages} onClose={() => setOpenCohortId(null)} />}
    </div>
  )
}
