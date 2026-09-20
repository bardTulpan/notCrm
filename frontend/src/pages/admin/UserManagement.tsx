import { useState } from 'react'
import { Avatar } from '../../components/Avatar'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { usersApi } from '../../api/users'
import { useToast, apiErrorMessage } from '../../hooks/useToast'
import { CreateUserModal } from './CreateUserModal'
import { ResetPasswordModal } from './ResetPasswordModal'
import type { UserDto } from '../../types'

export function UserManagement({ users, onChanged }: { users: UserDto[]; onChanged: () => void }) {
  const { push } = useToast()
  const [showCreate, setShowCreate] = useState(false)
  const [resetTarget, setResetTarget] = useState<UserDto | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<UserDto | null>(null)

  async function toggleBlock(user: UserDto) {
    try {
      if (user.status === 'ACTIVE') await usersApi.block(user.id)
      else await usersApi.unblock(user.id)
      onChanged()
    } catch (err) {
      push('error', apiErrorMessage(err))
    }
  }

  async function remove(user: UserDto) {
    try {
      await usersApi.remove(user.id)
      push('success', 'Пользователь удалён')
      onChanged()
    } catch (err) {
      push('error', apiErrorMessage(err))
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-2.5">
        <div className="font-display font-semibold text-sm">Пользователи</div>
        <button className="btn-primary" onClick={() => setShowCreate(true)}>
          + Пользователь
        </button>
      </div>
      {users.map((u) => (
        <div key={u.id} className={`card p-4 mb-2.5 max-w-[640px] ${u.status === 'BLOCKED' ? 'opacity-60' : ''}`}>
          <div className="flex items-center justify-between mb-2.5 flex-wrap gap-2">
            <div className="flex items-center gap-2.5">
              <Avatar name={u.fullName} color={u.avatarColor} />
              <span className="font-display font-semibold text-[13.5px]">{u.fullName}</span>
              <span
                className={`text-[10.5px] font-bold px-2 py-0.5 rounded-full ${
                  u.status === 'ACTIVE' ? 'bg-success-soft text-success' : 'bg-warn-soft text-warn'
                }`}
              >
                {u.status === 'ACTIVE' ? 'Активен' : 'Заблокирован'}
              </span>
              <span className="text-xs text-ink-400 font-mono">{u.role === 'ADMIN' ? 'админ' : 'куратор'}</span>
            </div>
            <span className="font-mono text-[11px] text-ink-600">логин: {u.username}</span>
          </div>
          <div className="flex items-center justify-between pt-2.5 border-t border-border">
            <button className="btn-ghost" onClick={() => setResetTarget(u)}>
              Сбросить пароль
            </button>
            <div className="flex gap-2">
              <button className={`btn-ghost ${u.status === 'ACTIVE' ? '!text-warn' : '!text-success'}`} onClick={() => toggleBlock(u)}>
                {u.status === 'ACTIVE' ? 'Заблокировать' : 'Разблокировать'}
              </button>
              <button className="btn-ghost !text-ink-400" onClick={() => setDeleteTarget(u)}>
                Удалить
              </button>
            </div>
          </div>
        </div>
      ))}

      {showCreate && <CreateUserModal onClose={() => setShowCreate(false)} onCreated={onChanged} />}
      {resetTarget && <ResetPasswordModal user={resetTarget} onClose={() => setResetTarget(null)} />}
      {deleteTarget && (
        <ConfirmDialog
          title="Удалить пользователя?"
          message={`${deleteTarget.fullName} будет деактивирован (мягкое удаление). Последнего активного админа или куратора с активными учениками удалить нельзя.`}
          confirmLabel="Удалить"
          danger
          onConfirm={() => remove(deleteTarget)}
          onClose={() => setDeleteTarget(null)}
        />
      )}
    </div>
  )
}
