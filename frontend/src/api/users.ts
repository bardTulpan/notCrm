import { client } from './client'
import type { CreateUserRequest, UpdateUserRequest, UserDto } from '../types'

export interface DirectoryEntry {
  id: string
  fullName: string
  avatarColor: string | null
  role: 'ADMIN' | 'CURATOR'
  blocked: boolean
}

export const usersApi = {
  directory: () => client.get<DirectoryEntry[]>('/directory/users').then((r) => r.data),

  list: () => client.get<UserDto[]>('/users').then((r) => r.data),

  get: (id: string) => client.get<UserDto>(`/users/${id}`).then((r) => r.data),

  create: (payload: CreateUserRequest) => client.post<UserDto>('/users', payload).then((r) => r.data),

  update: (id: string, payload: UpdateUserRequest) =>
    client.patch<UserDto>(`/users/${id}`, payload).then((r) => r.data),

  block: (id: string) => client.post<UserDto>(`/users/${id}/block`).then((r) => r.data),

  unblock: (id: string) => client.post<UserDto>(`/users/${id}/unblock`).then((r) => r.data),

  resetPassword: (id: string, newPassword: string) =>
    client.post(`/users/${id}/reset-password`, { newPassword }),

  remove: (id: string) => client.delete(`/users/${id}`),
}
