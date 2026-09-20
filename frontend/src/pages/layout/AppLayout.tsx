import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../../auth/useAuth'
import { Avatar } from '../../components/Avatar'

const TABS = [
  { to: '/leads', label: 'Лиды' },
  { to: '/students', label: 'Ученики' },
  { to: '/statistics', label: 'Статистика' },
]

export function AppLayout() {
  const { user, logout } = useAuth()
  const isAdmin = user?.role === 'ADMIN'

  return (
    <div className="min-h-screen flex flex-col">
      <div className="flex items-center justify-between px-6 py-4 border-b border-border bg-surface flex-wrap gap-3">
        <div className="font-display font-bold text-lg flex items-center gap-2">
          <span className="w-2.5 h-2.5 rounded-full bg-accent inline-block" />
          Пайплайн
        </div>
        <div className="flex gap-1 bg-bg p-1 rounded-[10px]">
          {[...TABS, ...(isAdmin ? [{ to: '/admin', label: 'Админка' }] : [])].map((tab) => (
            <NavLink
              key={tab.to}
              to={tab.to}
              className={({ isActive }) =>
                `border-none font-semibold text-[13px] px-4 py-2 rounded-lg cursor-pointer ${
                  isActive ? 'bg-surface text-ink-900 shadow-sm' : 'bg-transparent text-ink-600 hover:text-ink-900'
                }`
              }
            >
              {tab.label}
            </NavLink>
          ))}
        </div>
        <div className="flex items-center gap-3">
          {user && (
            <div className="flex items-center gap-2 text-xs font-semibold text-ink-600">
              <Avatar name={user.fullName} color={user.avatarColor} />
              {user.fullName}
              <span className="text-ink-400 font-normal">({user.role === 'ADMIN' ? 'Админ' : 'Куратор'})</span>
            </div>
          )}
          <button className="btn-ghost" onClick={() => logout()}>
            Выйти
          </button>
        </div>
      </div>
      <div className="p-6 flex-1 min-w-0">
        <Outlet />
      </div>
    </div>
  )
}
