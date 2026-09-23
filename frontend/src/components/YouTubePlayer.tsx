import { useCallback, useEffect, useRef } from 'react'
import { YOUTUBE_PLAYER_VARS } from '../lib/playback'

declare global {
  interface Window {
    YT?: any
    onYouTubeIframeAPIReady?: () => void
  }
}

interface YouTubePlayerProps {
  videoId: string | null
  playing: boolean
  startAtSeconds: number
  syncRequest?: { seconds: number; nonce: number } | null
  volume: number
  muted: boolean
  onEnded: () => void
  onError: () => void
}

let apiPromise: Promise<unknown> | null = null

function loadYouTubeApi(): Promise<unknown> {
  if (window.YT?.Player) {
    return Promise.resolve(window.YT)
  }
  if (!apiPromise) {
    apiPromise = new Promise((resolve) => {
      const previous = window.onYouTubeIframeAPIReady
      window.onYouTubeIframeAPIReady = () => {
        previous?.()
        resolve(window.YT)
      }
      const tag = document.createElement('script')
      tag.src = 'https://www.youtube.com/iframe_api'
      document.head.appendChild(tag)
    })
  }
  return apiPromise
}

export function YouTubePlayer({
  videoId,
  playing,
  startAtSeconds,
  syncRequest,
  volume,
  muted,
  onEnded,
  onError,
}: YouTubePlayerProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const playerRef = useRef<any>(null)
  const readyRef = useRef(false)
  const currentVideoRef = useRef<string | null>(null)
  const handlersRef = useRef({ onEnded, onError })
  const desiredRef = useRef({ videoId, playing, startAtSeconds, volume, muted })
  const syncRequestRef = useRef<{ seconds: number; nonce: number } | null | undefined>(syncRequest)
  const lastSyncNonceRef = useRef<number | null>(null)

  desiredRef.current = { videoId, playing, startAtSeconds, volume, muted }
  syncRequestRef.current = syncRequest

  useEffect(() => {
    handlersRef.current = { onEnded, onError }
  }, [onEnded, onError])

  const applyDesired = useCallback(() => {
    const player = playerRef.current
    const desired = desiredRef.current
    if (!player || !readyRef.current || !desired.videoId) {
      return
    }

    const sync = syncRequestRef.current

    if (sync != null && sync.nonce !== lastSyncNonceRef.current) {
      lastSyncNonceRef.current = sync.nonce
      currentVideoRef.current = desired.videoId
      player.loadVideoById(desired.videoId, Math.max(0, sync.seconds))
    } else if (currentVideoRef.current !== desired.videoId) {
      currentVideoRef.current = desired.videoId
      player.loadVideoById(desired.videoId, Math.max(0, desired.startAtSeconds))
    }

    if (desired.playing) {
      player.playVideo()
    } else {
      player.pauseVideo()
    }

    player.setVolume(Math.max(0, Math.min(100, desired.volume)))
    if (desired.muted) {
      player.mute()
    } else {
      player.unMute()
    }
  }, [])

  useEffect(() => {
    let cancelled = false

    void loadYouTubeApi().then(() => {
      if (cancelled || !containerRef.current) {
        return
      }
      playerRef.current = new window.YT.Player(containerRef.current, {
        playerVars: YOUTUBE_PLAYER_VARS,
        events: {
          onReady: () => {
            readyRef.current = true
            applyDesired()
          },
          onStateChange: (event: { data: number }) => {
            if (event.data === 0) {
              const data = playerRef.current?.getVideoData?.()
              if (data && data.video_id === currentVideoRef.current) {
                handlersRef.current.onEnded()
              }
            }
          },
          onError: () => {
            handlersRef.current.onError()
          },
        },
      })
    })

    return () => {
      cancelled = true
      try {
        playerRef.current?.destroy()
      } catch {
        // Ignora erros de destroy.
      }
      playerRef.current = null
      readyRef.current = false
      currentVideoRef.current = null
    }
  }, [applyDesired])

  useEffect(() => {
    applyDesired()
  }, [videoId, playing, startAtSeconds, syncRequest, volume, muted, applyDesired])

  return <div ref={containerRef} className="youtube-player" />
}
