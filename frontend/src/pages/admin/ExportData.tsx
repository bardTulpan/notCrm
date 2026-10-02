import { useState } from 'react'
import { studentsApi } from '../../api/students'
import { apiErrorMessage, useToast } from '../../hooks/useToast'
import { csvDateStamp, downloadCsv } from '../../utils/csv'
import { formatDate } from '../../utils/dates'
import type { CohortDto, StageDto, UserDto } from '../../types'

const HEALTH_LABEL = { green: 'по плану', yellow: 'внимание', red: 'застрял', paused: 'на паузе' } as const

/** Admin-only data export. Kept out of the everyday board on purpose. */
export function ExportData({ stages, cohorts, users }: { stages: StageDto[]; cohorts: CohortDto[]; users: UserDto[] }) {
  const { push } = useToast()
  const [curatorId, setCuratorId] = useState('')
  const [busy, setBusy] = useState(false)

  async function exportStudents() {
    setBusy(true)
    try {
      const students = await studentsApi.list({ curatorId: curatorId || undefined })
      const stageName = (id: string) => stages.find((s) => s.id === id)?.name ?? ''
      const cohortName = (id: string | null) => cohorts.find((c) => c.id === id)?.name ?? ''
      const curatorName = (id: string) => users.find((u) => u.id === id)?.fullName ?? ''
      downloadCsv(
        `ученики-${csvDateStamp()}.csv`,
        ['Имя', 'Telegram', 'Этап', 'Куратор', 'Когорта', 'Дата старта', 'На этапе с', 'Дней на этапе', 'Статус', 'Постоплата, %'],
        students.map((s) => [
          s.fullName,
          s.telegramUsername,
          stageName(s.currentStageId),
          curatorName(s.curatorId),
          cohortName(s.cohortId),
          formatDate(s.startedAt),
          formatDate(s.stageEnteredAt),
          s.daysOnStage,
          HEALTH_LABEL[s.health],
          s.postpayPercent,
        ]),
      )
      push('success', `Выгружено учеников: ${students.length}`)
    } catch (err) {
      push('error', apiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div>
      <div className="font-display font-semibold text-sm mb-2.5">Экспорт в CSV</div>
      <div className="card p-4 max-w-[480px] flex flex-col gap-3">
        <div className="text-[13px] font-medium">Список учеников</div>
        <div className="text-xs text-ink-600">Имя, Telegram, этап, куратор, когорта, даты, дни на этапе, статус, постоплата.</div>
        <div className="flex gap-2">
          <select className="input flex-1" value={curatorId} onChange={(e) => setCuratorId(e.target.value)}>
            <option value="">Все кураторы</option>
            {users
              .filter((u) => u.role === 'CURATOR')
              .map((u) => (
                <option key={u.id} value={u.id}>
                  {u.fullName}
                </option>
              ))}
          </select>
          <button className="btn-primary flex-shrink-0" onClick={exportStudents} disabled={busy}>
            Скачать CSV
          </button>
        </div>
        <div className="text-xs text-ink-400">Воронка по когортам и нагрузка кураторов выгружаются на странице «Статистика».</div>
      </div>
    </div>
  )
}
