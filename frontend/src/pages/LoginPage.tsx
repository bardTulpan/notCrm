import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { apiErrorMessage } from '../hooks/useToast'

export function LoginPage() {
  const { login, status } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (status === 'authenticated') {
    const from = (location.state as { from?: string } | null)?.from ?? '/leads'
    return <Navigate to={from} replace />
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (submitting) return
    setSubmitting(true)
    setError(null)
    try {
      await login(username, password)
      navigate('/leads', { replace: true })
    } catch (err) {
      const message = apiErrorMessage(err)
      const translated: Record<string, string> = {
        'Invalid credentials': 'Неверный логин или пароль',
        'Account is blocked': 'Аккаунт заблокирован администратором',
      }
      setError(translated[message] ?? message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-bg">
      <form onSubmit={onSubmit} className="card p-8 w-full max-w-[380px] flex flex-col gap-3">
        <div className="font-display font-bold text-lg flex items-center gap-2 mb-2">
          <span className="w-2.5 h-2.5 rounded-full bg-accent inline-block" />
          Пайплайн
        </div>
        <label className="text-xs font-semibold text-ink-600">
          Логин
          <input
            className="input w-full mt-1"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            required
          />
        </label>
        <label className="text-xs font-semibold text-ink-600">
          Пароль
          <input
            className="input w-full mt-1"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
          />
        </label>
        {error && <div className="text-xs text-warn">{error}</div>}
        <button className="btn-primary w-full mt-2" type="submit" disabled={submitting}>
          {submitting ? 'Входим…' : 'Войти'}
        </button>
      </form>
    </div>
  )
}
