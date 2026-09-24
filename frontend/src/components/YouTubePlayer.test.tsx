// @vitest-environment jsdom

import { act } from 'react'
import { createRoot } from 'react-dom/client'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { YouTubePlayer } from './YouTubePlayer'

(globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }).IS_REACT_ACT_ENVIRONMENT = true

describe('YouTubePlayer', () => {
  afterEach(() => {
    delete window.YT
    document.body.innerHTML = ''
  })

  it('desmonta sem erro quando a API do YouTube substitui o nó de montagem', async () => {
    const destroy = vi.fn()
    window.YT = {
      Player: vi.fn((mount: HTMLElement) => {
        const iframe = document.createElement('iframe')
        mount.replaceWith(iframe)
        return { destroy }
      }),
    }

    const host = document.createElement('div')
    document.body.appendChild(host)
    const root = createRoot(host)

    await act(async () => {
      root.render(
        <YouTubePlayer
          videoId="video-1"
          playing
          startAtSeconds={0}
          volume={100}
          muted={false}
          onEnded={vi.fn()}
          onError={vi.fn()}
        />,
      )
    })

    expect(() => {
      act(() => root.unmount())
    }).not.toThrow()
    expect(destroy).toHaveBeenCalledOnce()
  })
})
