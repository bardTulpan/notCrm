import { client } from './client'
import type { CohortDto, CreateCohortRequest, UpdateCohortRequest } from '../types'

export const cohortsApi = {
  list: () => client.get<CohortDto[]>('/cohorts').then((r) => r.data),

  create: (payload: CreateCohortRequest) => client.post<CohortDto>('/cohorts', payload).then((r) => r.data),

  update: (id: string, payload: UpdateCohortRequest) =>
    client.patch<CohortDto>(`/cohorts/${id}`, payload).then((r) => r.data),

  archive: (id: string) => client.post(`/cohorts/${id}/archive`),
}
