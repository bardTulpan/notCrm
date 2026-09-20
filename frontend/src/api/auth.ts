import { client } from './client'
import type { AuthResponse, AuthUserDto } from '../types'

export const authApi = {
  login: (username: string, password: string) =>
    client.post<AuthResponse>('/auth/login', { username, password }, { withCredentials: true }).then((r) => r.data),

  refresh: () =>
    client.post<AuthResponse>('/auth/refresh', undefined, { withCredentials: true }).then((r) => r.data),

  logout: () => client.post('/auth/logout', undefined, { withCredentials: true }),

  me: () => client.get<AuthUserDto>('/auth/me').then((r) => r.data),

  changePassword: (currentPassword: string, newPassword: string) =>
    client.patch('/auth/me/password', { currentPassword, newPassword }),
}
