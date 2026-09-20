import { client } from './client'
import type { ConvertLeadRequest, CreateLeadRequest, LeadDto, LeadStatus, StudentDto, UpdateLeadRequest } from '../types'

export interface LeadFilters {
  status?: LeadStatus
  search?: string
  assignedCuratorId?: string
  pingFrom?: string
  pingTo?: string
}

export const leadsApi = {
  list: (filters: LeadFilters = {}) =>
    client.get<LeadDto[]>('/leads', { params: filters }).then((r) => r.data),

  get: (id: string) => client.get<LeadDto>(`/leads/${id}`).then((r) => r.data),

  create: (payload: CreateLeadRequest) => client.post<LeadDto>('/leads', payload).then((r) => r.data),

  update: (id: string, payload: UpdateLeadRequest) =>
    client.patch<LeadDto>(`/leads/${id}`, payload).then((r) => r.data),

  archive: (id: string) => client.post(`/leads/${id}/archive`),

  restore: (id: string) => client.post(`/leads/${id}/restore`),

  postponePing: (id: string, nextPingAt: string) =>
    client.post<LeadDto>(`/leads/${id}/postpone-ping`, { nextPingAt }).then((r) => r.data),

  convert: (id: string, payload: ConvertLeadRequest) =>
    client.post<StudentDto>(`/leads/${id}/convert`, payload).then((r) => r.data),
}
