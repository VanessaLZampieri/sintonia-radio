import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import type { Client } from '@stomp/stompjs'
import { api, ApiError } from '../api'
import { YouTubePlayer } from '../components/YouTubePlayer'
import { clampSeekSeconds, formatDuration, parseDurationSeconds, resumeTargetSeconds, shouldPauseLocally, shouldPlay } from '../lib/playback'
import { connectToRoom, getOrCreateClientSessionId } from '../socket'
import type { HistoryItem, RoomState, SongSearchItem } from '../types'

export function RoomPage() {
  const { code = '' } = useParams()
  const navigate = useNavigate()

  const [roomId, setRoomId] = useState<number | null>(null)
  const [state, setState] = useState<RoomState | null>(null)
  const [history, setHistory] = useState<HistoryItem[]>([])
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<SongSearchItem[]>([])
  const [searching, setSearching] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)
  const [entered, setEntered] = useState(false)
  const [localPaused, setLocalPaused] = useState(false)
  const [syncRequest, setSyncRequest] = useState<{ seconds: number; nonce: number } | null>(null)

  const mySessionId = useMemo(() => getOrCreateClientSessionId(), [])
  const socketRef = useRef<Client | null>(null)
  const codeRef = useRef(code.trim().toUpperCase())

  const loadState = useCallback(async (id: number) => {
    setState(await api.roomState(id))
  }, [])

  const loadHistory = useCallback(async (id: number) => {
    setHistory(await api.history(id))
  }, [])

  useEffect(() => {
    let active = true

    async function init() {
      try {
        const member = await api.enterRoom(codeRef.current)
        if (!active) {
          return
        }
        setRoomId(member.roomId)
        await loadState(member.roomId)
        await loadHistory(member.roomId)
        setEntered(true)
      } catch (err) {
        if (active) {
          setError(err instanceof ApiError ? err.message : 'Não foi possível entrar na sala.')
        }
      }
    }

    void init()
    return () => {
      active = false
    }
  }, [loadState, loadHistory])

  useEffect(() => {
    if (!entered || !roomId) {
      return
    }
    const client = connectToRoom(
      codeRef.current,
      () => {
        void loadState(roomId)
        void loadHistory(roomId)
      },
      () => {
        void loadState(roomId)
        void loadHistory(roomId)
      },
    )
    socketRef.current = client

    return () => {
      client.deactivate()
      socketRef.current = null
    }
  }, [entered, roomId, loadState, loadHistory])

  const refresh = useCallback(() => {
    if (roomId) {
      void loadState(roomId)
      void loadHistory(roomId)
    }
  }, [roomId, loadState, loadHistory])

  useEffect(() => {
    setLocalPaused(false)
    setSyncRequest(null)
  }, [state?.playbackMode])

  const search = async () => {
    const trimmed = query.trim()
    if (!trimmed) {
      return
    }
    setSearching(true)
    setError(null)
    try {
      setResults(await api.searchSongs(trimmed))
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível buscar músicas.')
    } finally {
      setSearching(false)
    }
  }

  const addSong = async (videoId: string | null) => {
    if (!videoId || !roomId) {
      return
    }
    setError(null)
    try {
      const song = await api.selectSong(videoId)
      await api.addToQueue(roomId, song.id)
      setQuery('')
      setResults([])
      refresh()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível adicionar a música.')
    }
  }

  const removeItem = async (queueItemId: number) => {
    if (!roomId) {
      return
    }
    setError(null)
    try {
      await api.removeFromQueue(roomId, queueItemId)
      refresh()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível remover a música.')
    }
  }

  const claimPlayer = async () => {
    if (!roomId) {
      return
    }
    setError(null)
    try {
      await api.claimPlayer(roomId, mySessionId)
      refresh()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível assumir o player.')
    }
  }

  const releasePlayer = async () => {
    if (!roomId) {
      return
    }
    setError(null)
    try {
      await api.releasePlayer(roomId, mySessionId)
      refresh()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível liberar o player.')
    }
  }

  const changeMode = async (mode: 'TODOS_OS_NAVEGADORES' | 'CAIXA_DE_MUSICA') => {
    if (!roomId) {
      return
    }
    setError(null)
    try {
      await api.changePlaybackMode(roomId, mode)
      refresh()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível alterar o modo.')
    }
  }

  const voteSkip = async () => {
    if (!roomId) {
      return
    }
    setError(null)
    try {
      await api.skipVote(roomId)
      refresh()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível votar para pular.')
    }
  }

  const pause = async () => {
    if (!roomId || !currentPlayback) {
      return
    }
    if (state && shouldPauseLocally(state.playbackMode)) {
      setLocalPaused(true)
      return
    }
    setError(null)
    try {
      await api.pausePlayback(currentPlayback.playbackId, mySessionId)
      refresh()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível pausar.')
    }
  }

  const resume = async () => {
    if (!roomId || !currentPlayback) {
      return
    }
    if (state && shouldPauseLocally(state.playbackMode)) {
      setError(null)
      try {
        const fresh = await api.roomState(roomId)
        const target = fresh.currentPlayback
          ? resumeTargetSeconds(
              fresh.currentPlayback.positionSeconds,
              fresh.currentPlayback.song.duration,
            )
          : 0
        setState(fresh)
        setLocalPaused(false)
        setSyncRequest({ seconds: target, nonce: Date.now() })
      } catch (err) {
        setError(err instanceof ApiError ? err.message : 'Não foi possível retomar.')
      }
      return
    }
    setError(null)
    try {
      await api.resumePlayback(currentPlayback.playbackId, mySessionId)
      refresh()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível retomar.')
    }
  }

  const handleEnded = useCallback(() => {
    if (!roomId || !state?.currentPlayback?.playbackId) {
      return
    }
    if (state && shouldPauseLocally(state.playbackMode) && localPaused) {
      return
    }
    void api
      .finishPlayback(state.currentPlayback.playbackId, mySessionId)
      .then(refresh)
      .catch(() => {})
  }, [roomId, state, mySessionId, refresh, localPaused])

  const handlePlaybackError = useCallback(() => {
    if (!roomId || !state?.currentPlayback?.playbackId) {
      return
    }
    if (state && shouldPauseLocally(state.playbackMode) && localPaused) {
      return
    }
    void api
      .errorPlayback(state.currentPlayback.playbackId, mySessionId)
      .then(refresh)
      .catch(() => {})
  }, [roomId, state, mySessionId, refresh, localPaused])

  const copyCode = async () => {
    try {
      await navigator.clipboard.writeText(codeRef.current)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1500)
    } catch {
      setCopied(false)
    }
  }

  const leave = async () => {
    try {
      await api.leaveRoom(codeRef.current)
    } catch {
      // Ignora falha ao sair; a navegação continua.
    }
    navigate('/')
  }

  const isPlayer = state
    ? shouldPlay(state.playbackMode, state.player?.clientSessionId ?? null, mySessionId)
    : false

  const currentPlayback = state?.currentPlayback ?? null
  const isPaused = state && shouldPauseLocally(state.playbackMode)
    ? localPaused
    : (currentPlayback?.paused ?? false)
  const seekSeconds = currentPlayback
    ? clampSeekSeconds(
        currentPlayback.positionSeconds,
        parseDurationSeconds(currentPlayback.song.duration),
      )
    : 0

  return (
    <div className="container stack">
      <header className="row">
        <button className="btn btn-sm" onClick={() => navigate('/')}>
          ← Início
        </button>
        <h2 style={{ margin: 0 }} className="grow">
          Sala <span style={{ letterSpacing: 2 }}>{codeRef.current}</span>
        </h2>
        <button className="btn btn-sm" onClick={copyCode}>
          {copied ? 'Copiado' : 'Copiar código'}
        </button>
        <button className="btn btn-sm btn-danger" onClick={leave}>
          Sair
        </button>
      </header>

      {error && <div className="error-banner">{error}</div>}

      {!entered && !error && <div className="muted">Entrando na sala…</div>}

      {entered && state && (
        <>
          <div className="grid-2">
            <section className="card stack">
              <h3 style={{ margin: 0 }}>Reprodução</h3>

              <div className="row">
                <span className="muted">Modo:</span>
                <button
                  className="btn btn-sm"
                  onClick={() => void changeMode('TODOS_OS_NAVEGADORES')}
                  disabled={state.playbackMode === 'TODOS_OS_NAVEGADORES'}
                >
                  Todos os navegadores
                </button>
                <button
                  className="btn btn-sm"
                  onClick={() => void changeMode('CAIXA_DE_MUSICA')}
                  disabled={state.playbackMode === 'CAIXA_DE_MUSICA'}
                >
                  Caixa de música
                </button>
              </div>

              {state.playbackMode === 'CAIXA_DE_MUSICA' && (
                <div className="card stack" style={{ background: 'var(--panel-2)' }}>
                  <div className="row">
                    <span className="muted">
                      {state.player ? 'Player assumido' : 'Nenhum player assumido'}
                    </span>
                    {isPlayer ? (
                      <button className="btn btn-sm btn-danger" onClick={releasePlayer}>
                        Liberar player
                      </button>
                    ) : (
                      <button className="btn btn-sm" onClick={claimPlayer}>
                        Assumir player
                      </button>
                    )}
                  </div>
                </div>
              )}

              {currentPlayback && (
                <div className="stack">
                  <div className="row">
                    <img
                      className="thumb"
                      src={currentPlayback.song.thumbnailUrl}
                      alt={currentPlayback.song.title}
                      style={{ width: 96, height: 54, borderRadius: 6, objectFit: 'cover' }}
                    />
                    <div className="grow">
                      <div className="ellipsis">{currentPlayback.song.title}</div>
                      <div className="muted" style={{ fontSize: '0.85rem' }}>
                        {formatDuration(currentPlayback.song.duration)}
                        {isPaused ? ' · pausado' : ''}
                      </div>
                    </div>
                    {isPlayer && (
                      <button
                        className="btn btn-sm"
                        onClick={() => void (isPaused ? resume() : pause())}
                      >
                        {isPaused ? 'Retomar' : 'Pausar'}
                      </button>
                    )}
                  </div>

                  {isPlayer ? (
                    <YouTubePlayer
                      videoId={currentPlayback.song.youtubeVideoId}
                      playing={!isPaused}
                      startAtSeconds={seekSeconds}
                      syncRequest={syncRequest}
                      onEnded={handleEnded}
                      onError={handlePlaybackError}
                    />
                  ) : (
                    <div className="muted">
                      {isPaused
                        ? 'Reprodução pausada.'
                        : 'Aguarde — somente o player desta sala reproduz o áudio.'}
                    </div>
                  )}
                </div>
              )}

              {!currentPlayback && (
                <div className="muted">
                  {isPlayer ? 'Nada tocando no momento. Adicione músicas à fila.' : 'Nada tocando no momento.'}
                </div>
              )}

              {currentPlayback && state.skipVote && (
                <div className="card stack" style={{ background: 'var(--panel-2)' }}>
                  <div className="row">
                    <span className="grow">
                      Pular: {state.skipVote.votes}/{state.skipVote.requiredVotes} votos
                    </span>
                    <button
                      className="btn btn-sm"
                      onClick={voteSkip}
                      disabled={state.skipVote.currentUserVoted}
                    >
                      {state.skipVote.currentUserVoted ? 'Voto registrado' : 'Votar para pular'}
                    </button>
                  </div>
                </div>
              )}
            </section>

            <section className="card stack">
              <h3 style={{ margin: 0 }}>Fila</h3>

              <div className="row">
                <input
                  className="input grow"
                  placeholder="Buscar música no YouTube…"
                  value={query}
                  onChange={(event) => setQuery(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter') {
                      void search()
                    }
                  }}
                />
                <button className="btn" onClick={search} disabled={searching}>
                  {searching ? '…' : 'Buscar'}
                </button>
              </div>

              {results.length > 0 && (
                <div className="list">
                  {results.map((item) => (
                    <div className="list-item" key={item.videoId}>
                      <div className="grow ellipsis">{item.title}</div>
                      <button className="btn btn-sm" onClick={() => void addSong(item.videoId)}>
                        Adicionar
                      </button>
                    </div>
                  ))}
                </div>
              )}

              <div className="list">
                {state.queue.length === 0 && <div className="muted">A fila está vazia.</div>}
                {state.queue.map((item) => (
                  <div className="list-item" key={item.queueItemId}>
                    <img className="thumb" src={item.song.thumbnailUrl} alt={item.song.title} />
                    <div className="grow">
                      <div className="ellipsis">{item.song.title}</div>
                      <div className="muted" style={{ fontSize: '0.85rem' }}>
                        #{item.position} · {formatDuration(item.song.duration)}
                        {item.status === 'PLAYING' ? ' · tocando' : ''}
                      </div>
                    </div>
                    {item.status === 'WAITING' && (
                      <button
                        className="btn btn-sm btn-danger"
                        onClick={() => void removeItem(item.queueItemId)}
                      >
                        Remover
                      </button>
                    )}
                  </div>
                ))}
              </div>
            </section>
          </div>

          <section className="card stack">
            <h3 style={{ margin: 0 }}>Histórico</h3>
            <div className="list">
              {history.length === 0 && <div className="muted">Nenhuma reprodução ainda.</div>}
              {history.map((item) => (
                <div className="list-item" key={item.playbackId}>
                  <img className="thumb" src={item.song.thumbnailUrl} alt={item.song.title} />
                  <div className="grow">
                    <div className="ellipsis">{item.song.title}</div>
                    <div className="muted" style={{ fontSize: '0.85rem' }}>
                      {item.status} · {formatDuration(item.song.duration)}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </section>
        </>
      )}
    </div>
  )
}
