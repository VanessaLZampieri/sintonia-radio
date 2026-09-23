import type { ReactNode } from 'react'

export type NoticeType = 'error' | 'success' | 'info'

interface NoticeProps {
  type: NoticeType
  children: ReactNode
  onClose?: () => void
}

export function Notice({ type, children, onClose }: NoticeProps) {
  const role = type === 'error' ? 'alert' : 'status'
  return (
    <div className={`notice notice-${type}`} role={role}>
      <div className="grow">{children}</div>
      {onClose && (
        <button type="button" className="notice-close" aria-label="Fechar aviso" onClick={onClose}>
          ×
        </button>
      )}
    </div>
  )
}
