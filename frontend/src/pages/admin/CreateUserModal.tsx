import { useState } from 'react'
import { Modal } from '../../components/Modal'
import { usersApi } from '../../api/users'
import { useToast, apiErrorMessage } from '../../hooks/useToast'
import type { Role } from '../../types'

const PALETTE = ['#2F5EFF', '#17875A', '#C97A11', '#A23BAB', '#0F9AA5', '#D94F70']

function genPassword(): string {
  const chars = 'ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'
  let s = ''
  for (let i = 0; i < 8; i++) s += chars[Math.floor(Math.random() * chars.length)]
  return s
}

export function CreateUserModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const { push } = useToast()
  const [fullName, setFullName] = useState('')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState(genPassword())
  const [role, setRole] = useState<Role>('CURATOR')
  const [submitting, setSubmitting] = useState(false)
  const [created, setCreated] = useState<{ username: string; password: string } | null>(null)

  async function submit() {
    if (!fullName.trim() || !username.trim() || password.length < 8) return
    setSubmitting(true)
    try {
      await usersApi.create({
        fullName: fullName.trim(),
        username: username.trim(),
        password,
        role,
        avatarColor: PALETTE[Math.floor(Math.random() * PALETTE.length)],
      })
      setCreated({ username: username.trim(), password })
      onCreated()
    } catch (err) {
      push('error', apiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  if (created) {
    return (
      <Modal title="Пользователь создан" onClose={onClose}>
        <div className="text-sm mb-3">
          Сохраните пароль сейчас — он больше нигде не будет показан.
        </div>
        <div className="card p-3 font-mono text-[13px] flex flex-col gap-1 mb-4">
          <div>логин: {created.username}</div>
          <div>пароль: {created.password}</div>
        </div>
        <button className="btn-primary w-full" onClick={onClose}>
          Готово
        </button>
      </Modal>
    )
  }

  return (
    <Modal title="Новый пользователь" onClose={onClose}>
      <div className="flex flex-col gap-3">
        <label className="text-xs font-semibold text-ink-600">
          Имя
          <input className="input w-full mt-1" value={fullName} onChange={(e) => setFullName(e.target.value)} />
        </label>
        <label className="text-xs font-semibold text-ink-600">
          Логин
          <input className="input w-full mt-1" value={username} onChange={(e) => setUsername(e.target.value)} />
        </label>
        <label className="text-xs font-semibold text-ink-600">
          Роль
          <select className="input w-full mt-1" value={role} onChange={(e) => setRole(e.target.value as Role)}>
            <option value="CURATOR">Куратор</option>
            <option value="ADMIN">Админ</option>
          </select>
        </label>
        <label className="text-xs font-semibold text-ink-600">
          Пароль
          <div className="flex gap-1.5 mt-1">
            <input className="input flex-1" value={password} onChange={(e) => setPassword(e.target.value)} />
            <button type="button" className="btn-ghost" onClick={() => setPassword(genPassword())}>
              Сгенерировать
            </button>
          </div>
        </label>
        <div className="flex justify-end gap-2 mt-2">
          <button className="btn-ghost" onClick={onClose}>
            Отмена
          </button>
          <button className="btn-primary" onClick={submit} disabled={submitting || !fullName.trim() || !username.trim() || password.length < 8}>
            Создать
          </button>
        </div>
      </div>
    </Modal>
  )
}
