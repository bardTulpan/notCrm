import { client } from './client'
import type { AuditPage } from '../types'

export interface AuditFilters {
  actorId?: string
  entityType?: string
  entityId?: string
  from?: string
  to?: string
  page?: number
  size?: number
}

export const auditApi = {
  list: (filters: AuditFilters = {}) => client.get<AuditPage>('/audit-log', { params: filters }).then((r) => r.data),
}
