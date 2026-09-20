import { Modal } from './Modal'

export function ConfirmDialog({
  title,
  message,
  confirmLabel = 'Подтвердить',
  danger = false,
  onConfirm,
  onClose,
}: {
  title: string
  message: string
  confirmLabel?: string
  danger?: boolean
  onConfirm: () => void
  onClose: () => void
}) {
  return (
    <Modal title={title} onClose={onClose}>
      <p className="text-sm text-ink-600 mb-4">{message}</p>
      <div className="flex justify-end gap-2">
        <button className="btn-ghost" onClick={onClose}>
          Отмена
        </button>
        <button
          className={danger ? 'btn-danger' : 'btn-primary'}
          onClick={() => {
            onConfirm()
            onClose()
          }}
        >
          {confirmLabel}
        </button>
      </div>
    </Modal>
  )
}
