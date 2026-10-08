import { client } from './client'
import type { CohortStats, CuratorStats, CuratorWorkload, OverdueStudent, StageStats, StatsOverview } from '../types'

// Comma-joined so Spring binds it to List<UUID> (axios would otherwise send `curatorIds[]=`).
function byCurators(curatorIds?: string[]) {
  return curatorIds && curatorIds.length > 0 ? { params: { curatorIds: curatorIds.join(',') } } : undefined
}

export const statisticsApi = {
  overview: (curatorIds?: string[]) => client.get<StatsOverview>('/stats/overview', byCurators(curatorIds)).then((r) => r.data),

  stages: (curatorIds?: string[]) => client.get<StageStats[]>('/stats/stages', byCurators(curatorIds)).then((r) => r.data),

  curators: (curatorIds?: string[]) => client.get<CuratorStats[]>('/stats/curators', byCurators(curatorIds)).then((r) => r.data),

  curatorWorkload: () => client.get<CuratorWorkload[]>('/stats/curator-workload').then((r) => r.data),

  overdueStudents: (curatorIds?: string[]) =>
    client.get<OverdueStudent[]>('/stats/overdue-students', byCurators(curatorIds)).then((r) => r.data),

  cohorts: () => client.get<CohortStats[]>('/stats/cohorts').then((r) => r.data),

  cohortDetail: (id: string) => client.get<CohortStats>(`/stats/cohorts/${id}`).then((r) => r.data),
}
