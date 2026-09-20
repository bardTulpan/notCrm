import { client } from './client'
import type {
  CommentDto,
  CreateStudentRequest,
  StudentDto,
  StudentHistoryDto,
  UpdateStudentRequest,
} from '../types'

export interface StudentFilters {
  stageId?: string
  curatorId?: string
  cohortId?: string
  onlyOverdue?: boolean
  search?: string
}

export const studentsApi = {
  list: (filters: StudentFilters = {}) =>
    client.get<StudentDto[]>('/students', { params: filters }).then((r) => r.data),

  get: (id: string) => client.get<StudentDto>(`/students/${id}`).then((r) => r.data),

  create: (payload: CreateStudentRequest) => client.post<StudentDto>('/students', payload).then((r) => r.data),

  update: (id: string, payload: UpdateStudentRequest) =>
    client.patch<StudentDto>(`/students/${id}`, payload).then((r) => r.data),

  moveStage: (id: string, stageId: string) =>
    client.post<StudentDto>(`/students/${id}/move-stage`, { stageId }).then((r) => r.data),

  pause: (id: string) => client.post<StudentDto>(`/students/${id}/pause`).then((r) => r.data),

  resume: (id: string) => client.post<StudentDto>(`/students/${id}/resume`).then((r) => r.data),

  assignCurator: (id: string, curatorId: string) =>
    client.post<StudentDto>(`/students/${id}/assign-curator`, { curatorId }).then((r) => r.data),

  history: (id: string) => client.get<StudentHistoryDto>(`/students/${id}/history`).then((r) => r.data),

  comments: (id: string) => client.get<CommentDto[]>(`/students/${id}/comments`).then((r) => r.data),

  addComment: (id: string, text: string) =>
    client.post<CommentDto>(`/students/${id}/comments`, { text }).then((r) => r.data),

  updateComment: (id: string, commentId: string, text: string) =>
    client.patch<CommentDto>(`/students/${id}/comments/${commentId}`, { text }).then((r) => r.data),

  deleteComment: (id: string, commentId: string) => client.delete(`/students/${id}/comments/${commentId}`),
}
