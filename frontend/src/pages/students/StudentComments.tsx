import { useEffect, useState } from 'react'
import { studentsApi } from '../../api/students'
import { useAuth } from '../../auth/useAuth'
import { useToast, apiErrorMessage } from '../../hooks/useToast'
import { formatDateShort } from '../../utils/dates'
import type { CommentDto } from '../../types'

export function StudentComments({ studentId, authorName }: { studentId: string; authorName: (id: string) => string }) {
  const { user } = useAuth()
  const { push } = useToast()
  const [comments, setComments] = useState<CommentDto[]>([])
  const [loading, setLoading] = useState(true)
  const [draft, setDraft] = useState('')
  const [editing, setEditing] = useState<{ id: string; text: string } | null>(null)

  function refetch() {
    setLoading(true)
    studentsApi
      .comments(studentId)
      .then(setComments)
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    refetch()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [studentId])

  async function add() {
    if (!draft.trim()) return
    try {
      await studentsApi.addComment(studentId, draft.trim())
      setDraft('')
      refetch()
    } catch (err) {
      push('error', apiErrorMessage(err))
    }
  }

  async function saveEdit() {
    if (!editing || !editing.text.trim()) return
    try {
      await studentsApi.updateComment(studentId, editing.id, editing.text.trim())
      setEditing(null)
      refetch()
    } catch (err) {
      push('error', apiErrorMessage(err))
    }
  }

  async function remove(id: string) {
    try {
      await studentsApi.deleteComment(studentId, id)
      refetch()
    } catch (err) {
      push('error', apiErrorMessage(err))
    }
  }

  return (
    <div>
      {loading && <div className="text-xs text-ink-400">Загрузка…</div>}
      {!loading && comments.length === 0 && <div className="text-xs text-ink-400 mb-2">Комментариев пока нет.</div>}
      {comments.map((c) => {
        const canManage = user?.role === 'ADMIN' || user?.id === c.authorId
        return (
          <div key={c.id} className="text-[13px] mb-2 pb-2 border-b border-border">
            <div className="text-ink-400 text-[11px] mb-0.5 flex items-center gap-2">
              <span>
                {authorName(c.authorId)} · {formatDateShort(c.createdAt)}
              </span>
              {canManage && editing?.id !== c.id && (
                <span className="flex gap-1.5">
                  <button className="underline" onClick={() => setEditing({ id: c.id, text: c.text })}>
                    ред.
                  </button>
                  <button className="underline text-warn" onClick={() => remove(c.id)}>
                    удалить
                  </button>
                </span>
              )}
            </div>
            {editing?.id === c.id ? (
              <div className="flex gap-1.5">
                <input
                  className="input flex-1"
                  value={editing.text}
                  onChange={(e) => setEditing({ id: c.id, text: e.target.value })}
                />
                <button className="btn-ghost" onClick={saveEdit}>
                  ОК
                </button>
              </div>
            ) : (
              <div>{c.text}</div>
            )}
          </div>
        )
      })}
      <div className="flex gap-1.5 mt-1.5">
        <input
          className="input flex-1"
          placeholder="Написать комментарий"
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') add()
          }}
        />
        <button className="btn-primary flex-shrink-0" onClick={add}>
          +
        </button>
      </div>
    </div>
  )
}
