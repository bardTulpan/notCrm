import { client } from './client'
import type { CreateStageRequest, StageDto, UpdateStageRequest } from '../types'

export const stagesApi = {
  list: () => client.get<StageDto[]>('/pipeline-stages').then((r) => r.data),

  create: (payload: CreateStageRequest) => client.post<StageDto>('/pipeline-stages', payload).then((r) => r.data),

  update: (id: string, payload: UpdateStageRequest) =>
    client.patch<StageDto>(`/pipeline-stages/${id}`, payload).then((r) => r.data),

  reorder: (ids: string[]) => client.put('/pipeline-stages/order', { ids }),

  archive: (id: string) => client.post(`/pipeline-stages/${id}/archive`),
}
