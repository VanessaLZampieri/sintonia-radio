import { useEffect, useRef } from 'react'
import type { RoomSummary } from '../types'
import { Avatar } from './Avatar'

export function RoomSummaryDialog({
  summary,
  loading,
  onClose,
}: {
  summary: RoomSummary | null
  loading: boolean
  onClose: () => void
}) {
  const dialogRef = useRef<HTMLElement>(null)
  const closeButtonRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    const previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null
    closeButtonRef.current?.focus()
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose()
        return
      }
      if (event.key !== 'Tab' || !dialogRef.current) {
        return
      }
      const focusable = Array.from(dialogRef.current.querySelectorAll<HTMLElement>('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'))
      if (focusable.length === 0) {
        event.preventDefault()
        return
      }
      const first = focusable[0]
      const last = focusable[focusable.length - 1]
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault()
        last.focus()
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault()
        first.focus()
      }
    }
    window.addEventListener('keydown', closeOnEscape)
    return () => {
      window.removeEventListener('keydown', closeOnEscape)
      previousFocus?.focus()
    }
  }, [onClose])

  const metrics = summary
    ? [
        [summary.playbackCount, 'Músicas tocaram'],
        [summary.participantCount, 'Pessoas participaram'],
        [summary.skippedCount, 'Músicas puladas'],
        [summary.autoDjPlaybackCount, 'Tocadas pelo Auto-DJ'],
        [summary.skipVoteCount, 'Votos de skip'],
      ] as const
    : []

  return (
    <div className="summary-backdrop" role="presentation" onMouseDown={(event) => {
      if (event.target === event.currentTarget) {
        onClose()
      }
    }}>
      <section ref={dialogRef} className="summary-dialog" role="dialog" aria-modal="true" aria-labelledby="room-summary-title">
        <header className="summary-header">
          <div>
            <span className="summary-kicker">A sessão em números</span>
            <h2 id="room-summary-title">Resumo da sala</h2>
          </div>
          <button ref={closeButtonRef} className="btn btn-sm btn-ghost" type="button" onClick={onClose} aria-label="Fechar resumo">
            Fechar
          </button>
        </header>

        {loading && <div className="summary-loading muted">Preparando o resumo…</div>}

        {!loading && summary && (
          <>
            <div className="summary-metrics">
              {metrics.map(([value, label]) => (
                <div className="summary-metric" key={label}>
                  <strong>{value}</strong>
                  <span>{label}</span>
                </div>
              ))}
            </div>

            <div className="summary-contributions">
              <h3>Contribuições</h3>
              {summary.contributions.length === 0 && (
                <div className="muted">As contribuições aparecerão quando a música começar.</div>
              )}
              {summary.contributions.map((contribution) => (
                <article className="contribution-row" key={contribution.userId}>
                  <Avatar
                    userId={contribution.userId}
                    displayName={contribution.displayName}
                    avatarUrl={contribution.avatarUrl}
                  />
                  <div className="contribution-content">
                    <strong>{contribution.displayName}</strong>
                    <div className="contribution-stats">
                      <span><b>{contribution.addedCount}</b> adicionadas</span>
                      <span><b>{contribution.playedCount}</b> tocaram</span>
                      <span><b>{contribution.skippedCount}</b> puladas</span>
                      <span><b>{contribution.skipVoteCount}</b> votos</span>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          </>
        )}
      </section>
    </div>
  )
}
