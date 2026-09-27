// @vitest-environment jsdom

import { act, createElement } from 'react'
import { createRoot } from 'react-dom/client'
import { describe, expect, it, vi } from 'vitest'
import type { RoomSummary } from '../types'
import { RoomSummaryDialog } from './RoomSummaryDialog'

(globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }).IS_REACT_ACT_ENVIRONMENT = true

describe('RoomSummaryDialog', () => {
  it('mostra métricas e contribuições sem ranking', () => {
    const summary: RoomSummary = {
      roomId: 1,
      playbackCount: 24,
      participantCount: 2,
      skippedCount: 3,
      autoDjPlaybackCount: 6,
      skipVoteCount: 11,
      contributions: [
        { userId: 1, displayName: 'Vanessa', avatarUrl: null, addedCount: 8, playedCount: 5, skippedCount: 2, skipVoteCount: 4 },
        { userId: 2, displayName: 'Marina', avatarUrl: null, addedCount: 6, playedCount: 4, skippedCount: 1, skipVoteCount: 2 },
      ],
    }
    const host = document.createElement('div')
    const root = createRoot(host)

    act(() => root.render(createElement(RoomSummaryDialog, {
      summary,
      loading: false,
      onClose: vi.fn(),
    })))

    expect(host.textContent).toContain('24')
    expect(host.textContent).toContain('Auto-DJ tocou')
    expect(host.textContent).toContain('Vanessa')
    expect(host.textContent).toContain('Marina')
    expect(host.querySelectorAll('.contribution-row')).toHaveLength(2)
    expect(host.querySelectorAll('.contribution-row .avatar-wave')).toHaveLength(2)
    expect(host.textContent).not.toContain('1º')
    act(() => root.unmount())
  })

  it('fecha com Escape', () => {
    const onClose = vi.fn()
    const opener = document.createElement('button')
    document.body.appendChild(opener)
    opener.focus()
    const host = document.createElement('div')
    document.body.appendChild(host)
    const root = createRoot(host)
    act(() => root.render(createElement(RoomSummaryDialog, {
      summary: null,
      loading: true,
      onClose,
    })))

    expect(document.activeElement).toBe(host.querySelector('[aria-label="Fechar resumo"]'))
    act(() => window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' })))

    expect(onClose).toHaveBeenCalledOnce()
    act(() => root.unmount())
    expect(document.activeElement).toBe(opener)
  })
})
