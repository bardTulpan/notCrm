import { useState } from 'react'
import { Modal } from '../../components/Modal'
import { usersApi } from '../../api/users'
import { useToast, apiErrorMessage } from '../../hooks/useToast'
import type { UserDto } from '../../types'

function genPassword(): string {
  const chars = 'ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'
  let s = ''
  for (let i = 0; i < 8; i++) s += chars[Math.floor(Math.random() * chars.length)]
  return s
}

export function ResetPasswordModal({ user, onClose }: { user: UserDto; onClose: () => void }) {
  const { push } = useToast()
  const [password, setPassword] = useState(genPassword())
  const [done, setDone] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  async function submit() {
    if (password.length < 8) return
    setSubmitting(true)
    try {
      await usersApi.resetPassword(user.id, password)
      setDone(true)
    } catch (err) {
      push('error', apiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  if (done) {
    return (
      <Modal title="Пароль сброшен" onClose={onClose}>
        <div className="text-sm mb-3">Новый пароль для {user.username} — сохраните его сейчас, повторно он не показывается.</div>
        <div className="card p-3 font-mono text-[13px] mb-4">{password}</div>
        <button className="btn-primary w-full" onClick={onClose}>
          Готово
        </button>
      </Modal>
    )
  }

  return (
    <Modal title={`Сбросить пароль: ${user.fullName}`} onClose={onClose}>
      <div className="flex gap-1.5 mb-4">
        <input className="input flex-1" value={password} onChange={(e) => setPassword(e.target.value)} />
        <button type="button" className="btn-ghost" onClick={() => setPassword(genPassword())}>
          Сгенерировать
        </button>
      </div>
      <div className="flex justify-end gap-2">
        <button className="btn-ghost" onClick={onClose}>
          Отмена
        </button>
        <button className="btn-primary" onClick={submit} disabled={submitting || password.length < 8}>
          Сбросить
        </button>
      </div>
    </Modal>
  )
}
