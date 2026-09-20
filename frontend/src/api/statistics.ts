import { client } from './client'
import type { CohortStats, CuratorStats, OverdueStudent, StageStats, StatsOverview } from '../types'

export const statisticsApi = {
  overview: () => client.get<StatsOverview>('/stats/overview').then((r) => r.data),

  stages: () => client.get<StageStats[]>('/stats/stages').then((r) => r.data),

  curators: () => client.get<CuratorStats[]>('/stats/curators').then((r) => r.data),

  overdueStudents: () => client.get<OverdueStudent[]>('/stats/overdue-students').then((r) => r.data),

  cohorts: () => client.get<CohortStats[]>('/stats/cohorts').then((r) => r.data),

  cohortDetail: (id: string) => client.get<CohortStats>(`/stats/cohorts/${id}`).then((r) => r.data),
}
