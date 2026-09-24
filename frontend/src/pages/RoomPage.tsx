import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import type { Client } from '@stomp/stompjs'
import { api, ApiError } from '../api'
import { YouTubePlayer } from '../components/YouTubePlayer'
import { Notice } from '../components/Notice'
import { Avatar } from '../components/Avatar'
import { Brand } from '../components/Brand'
import { Equalizer } from '../components/Equalizer'
import { RoomSummaryDialog } from '../components/RoomSummaryDialog'
import { clampSeekSeconds, formatDuration, formatSeconds, isPausedState, parseDurationSeconds, resumeTargetSeconds, shouldPauseLocally, shouldPlay } from '../lib/playback'
import { notifyFailure, notifySuccess } from '../lib/notify'
import { connectToRoom, getOrCreateClientSessionId } from '../socket'
import type { HistoryItem, RoomActivity, RoomActivityType, RoomState, RoomSummary, SongSearchItem } from '../types'

type MobileTab = 'sala' | 'buscar' | 'pessoas' | 'atividade'

function activityGlyph(type: RoomActivityType): string {
  switch (type) {
    case 'MEMBER_JOINED':
      return '+'
    case 'MEMBER_LEFT':
      return '−'
    case 'SONG_ADDED':
      return '♪'
    case 'SONG_REMOVED':
      return '−'
    case 'ROOM_RENAMED':
      return '✎'
    case 'PLAYBACK_STARTED':
      return '▶'
    case 'PLAYBACK_FINISHED':
      return '■'
    case 'PLAYBACK_SKIPPED':
      return '⏭'
    default:
      return '•'
  }
}

export function RoomPage() {
  const { code = '' } = useParams()
  const navigate = useNavigate()

  const [roomId, setRoomId] = useState<number | null>(null)
  const [state, setState] = useState<RoomState | null>(null)
  const [history, setHistory] = useState<HistoryItem[]>([])
  const [activities, setActivities] = useState<RoomActivity[]>([])
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<SongSearchItem[]>([])
  const [searching, setSearching] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)
  const [entered, setEntered] = useState(false)
  const [localPaused, setLocalPaused] = useState(false)
  const [syncRequest, setSyncRequest] = useState<{ seconds: number; nonce: number } | null>(null)
  const [roomName, setRoomName] = useState('')
  const [renaming, setRenaming] = useState(false)
  const [volume, setVolume] = useState(100)
  const [muted, setMuted] = useState(false)
  const [playerError, setPlayerError] = useState(false)
  const [mobileTab, setMobileTab] = useState<MobileTab>('sala')
  const [summary, setSummary] = useState<RoomSummary | null>(null)
  const [summaryOpen, setSummaryOpen] = useState(false)
  const [summaryLoading, setSummaryLoading] = useState(false)

  const mySessionId = useMemo(() => getOrCreateClientSessionId(), [])
  const socketRef = useRef<Client | null>(null)
  const codeRef = useRef(code.trim().toUpperCase())
  const summaryRequestRef = useRef(0)

  const loadState = useCallback(async (id: number) => {
    setState(await api.roomState(id))
  }, [])

  const loadHistory = useCallback(async (id: number) => {
    setHistory(await api.history(id))
  }, [])

  const loadActivities = useCallback(async (id: number) => {
    setActivities(await api.roomActivities(id))
  }, [])

  useEffect(() => {
    let active = true

    async function init() {
      try {
        const member = await api.enterRoom(codeRef.current, mySessionId)
        if (!active) {
          return
        }
        setRoomId(member.roomId)
        await loadState(member.roomId)
        await loadHistory(member.roomId)
        await loadActivities(member.roomId)
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
  }, [loadState, loadHistory, loadActivities, mySessionId])

  useEffect(() => {
    if (!entered || !roomId) {
      return
    }
    const client = connectToRoom(
      codeRef.current,
      () => {
        void api.renewPresence(codeRef.current, mySessionId)
        void loadState(roomId)
        void loadHistory(roomId)
        void loadActivities(roomId)
      },
      () => {
        void loadState(roomId)
        void loadHistory(roomId)
        void loadActivities(roomId)
      },
    )
    socketRef.current = client

    return () => {
      client.deactivate()
      socketRef.current = null
    }
  }, [entered, roomId, loadState, loadHistory, loadActivities, mySessionId])

  useEffect(() => {
    if (!entered) {
      return
    }
    const heartbeat = window.setInterval(() => {
      void api.renewPresence(codeRef.current, mySessionId).catch(() => {})
    }, 30000)
    return () => window.clearInterval(heartbeat)
  }, [entered, mySessionId])

  const refresh = useCallback(() => {
    if (roomId) {
      void loadState(roomId)
      void loadHistory(roomId)
      void loadActivities(roomId)
    }
  }, [roomId, loadState, loadHistory, loadActivities])

  useEffect(() => {
    setLocalPaused(false)
    setSyncRequest(null)
  }, [state?.playbackMode])

  useEffect(() => {
    if (state?.name) {
      setRoomName(state.name)
    }
  }, [state?.name])

  useEffect(() => {
    setPlayerError(false)
  }, [state?.currentPlayback?.playbackId])

  const closeSummary = useCallback(() => {
    summaryRequestRef.current += 1
    setSummaryOpen(false)
    setSummaryLoading(false)
  }, [])

  const openSummary = async () => {
    if (!roomId) {
      return
    }
    const requestId = ++summaryRequestRef.current
    setSummaryOpen(true)
    setSummaryLoading(true)
    try {
      const nextSummary = await api.roomSummary(roomId)
      if (requestId === summaryRequestRef.current) {
        setSummary(nextSummary)
      }
    } catch (err) {
      if (requestId === summaryRequestRef.current) {
        setSummaryOpen(false)
        notifyFailure(err instanceof ApiError ? err.message : 'Não foi possível carregar o resumo da sala.')
      }
    } finally {
      if (requestId === summaryRequestRef.current) {
        setSummaryLoading(false)
      }
    }
  }

  const search = async () => {
    const trimmed = query.trim()
    if (!trimmed) {
      return
    }
    setSearching(true)
    try {
      setResults(await api.searchSongs(trimmed))
    } catch (err) {
      notifyFailure(err instanceof ApiError ? err.message : 'Não foi possível buscar músicas.')
    } finally {
      setSearching(false)
    }
  }

  const addSong = async (videoId: string | null) => {
    if (!videoId || !roomId) {
      return
    }
    try {
      const song = await api.selectSong(videoId)
      await api.addToQueue(roomId, song.id)
      setQuery('')
      setResults([])
      notifySuccess('Música adicionada à fila.')
      refresh()
    } catch (err) {
      notifyFailure(err instanceof ApiError ? err.message : 'Não foi possível adicionar a música.')
    }
  }

  const removeItem = async (queueItemId: number) => {
    if (!roomId) {
      return
    }
    try {
      await api.removeFromQueue(roomId, queueItemId)
      refresh()
    } catch (err) {
      notifyFailure(err instanceof ApiError ? err.message : 'Não foi possível remover a música.')
    }
  }

  const claimPlayer = async () => {
    if (!roomId) {
      return
    }
    try {
      await api.claimPlayer(roomId, mySessionId)
      notifySuccess('Você assumiu a caixa.')
      refresh()
    } catch (err) {
      notifyFailure(err instanceof ApiError ? err.message : 'Não foi possível assumir o player.')
    }
  }

  const releasePlayer = async () => {
    if (!roomId) {
      return
    }
    try {
      await api.releasePlayer(roomId, mySessionId)
      notifySuccess('Você liberou a caixa.')
      refresh()
    } catch (err) {
      notifyFailure(err instanceof ApiError ? err.message : 'Não foi possível liberar o player.')
    }
  }

  const changeMode = async (mode: 'TODOS_OS_NAVEGADORES' | 'CAIXA_DE_MUSICA') => {
    if (!roomId) {
      return
    }
    try {
      await api.changePlaybackMode(roomId, mode)
      refresh()
    } catch (err) {
      notifyFailure(err instanceof ApiError ? err.message : 'Não foi possível alterar o modo.')
    }
  }

  const renameRoom = async () => {
    if (!roomId) {
      return
    }
    try {
      await api.renameRoom(roomId, roomName)
      setRenaming(false)
      notifySuccess('Nome da sala alterado.')
      refresh()
    } catch (err) {
      notifyFailure(err instanceof ApiError ? err.message : 'Não foi possível renomear a sala.')
    }
  }

  const voteSkip = async () => {
    if (!roomId) {
      return
    }
    try {
      await api.skipVote(roomId)
      notifySuccess('Voto registrado.')
      refresh()
    } catch (err) {
      notifyFailure(err instanceof ApiError ? err.message : 'Não foi possível votar para pular.')
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
    try {
      await api.pausePlayback(currentPlayback.playbackId, mySessionId)
      refresh()
    } catch (err) {
      notifyFailure(err instanceof ApiError ? err.message : 'Não foi possível pausar.')
    }
  }

  const resume = async () => {
    if (!roomId || !currentPlayback) {
      return
    }
    if (state && shouldPauseLocally(state.playbackMode)) {
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
        notifyFailure(err instanceof ApiError ? err.message : 'Não foi possível retomar.')
      }
      return
    }
    try {
      await api.resumePlayback(currentPlayback.playbackId, mySessionId)
      refresh()
    } catch (err) {
      notifyFailure(err instanceof ApiError ? err.message : 'Não foi possível retomar.')
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
    setPlayerError(true)
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
      await api.leaveRoom(codeRef.current, mySessionId)
    } catch {
      // Ignora falha ao sair; a navegação continua.
    }
    navigate('/')
  }

  const currentPlayback = state?.currentPlayback ?? null
  const hasPlayback = currentPlayback != null
  const globallyPaused = currentPlayback?.paused ?? false
  const isCaixaMode = state?.playbackMode === 'CAIXA_DE_MUSICA'
  const caixaAssigned = state?.player?.clientSessionId != null
  const isPlayer = state
    ? shouldPlay(state.playbackMode, state.player?.clientSessionId ?? null, mySessionId)
    : false
  const isPaused = state
    ? isPausedState(state.playbackMode, localPaused, globallyPaused)
    : false

  const durationSeconds = currentPlayback ? parseDurationSeconds(currentPlayback.song.duration) : null
  const seekSeconds = currentPlayback
    ? clampSeekSeconds(currentPlayback.positionSeconds, durationSeconds)
    : 0

  const lastSnapshotAtRef = useRef(Date.now())
  useEffect(() => {
    lastSnapshotAtRef.current = Date.now()
  }, [state])

  const [, setProgressTick] = useState(0)
  useEffect(() => {
    if (!hasPlayback || globallyPaused) {
      return
    }
    const id = window.setInterval(() => setProgressTick((t) => t + 1), 1000)
    return () => window.clearInterval(id)
  }, [hasPlayback, globallyPaused, currentPlayback?.playbackId])

  const displayedSeconds = currentPlayback
    ? clampSeekSeconds(
        globallyPaused
          ? currentPlayback.positionSeconds
          : currentPlayback.positionSeconds + Math.floor((Date.now() - lastSnapshotAtRef.current) / 1000),
        durationSeconds,
      )
    : 0

  const progressPercent = durationSeconds
    ? Math.min(100, Math.max(0, (displayedSeconds / durationSeconds) * 100))
    : 0

  const votePercent = state?.skipVote
    ? Math.min(100, Math.round((state.skipVote.votes / Math.max(1, state.skipVote.requiredVotes)) * 100))
    : 0

  const tabClass = (tab: MobileTab) => (mobileTab === tab ? 'is-mobile-active' : '')

  const showResults = results.length > 0

  const clearSearch = () => {
    setQuery('')
    setResults([])
  }

  const activityLine = (activity: RoomActivity) => {
    switch (activity.type) {
      case 'MEMBER_JOINED':
        return (
          <span>
            <strong>{activity.actorDisplayName}</strong> entrou na sala
          </span>
        )
      case 'MEMBER_LEFT':
        return (
          <span>
            <strong>{activity.actorDisplayName}</strong> saiu da sala
          </span>
        )
      case 'SONG_ADDED':
        return (
          <div className="stack" style={{ gap: 2 }}>
            <span>
              <strong>{activity.actorDisplayName}</strong> adicionou
            </span>
            <span className="activity-song">{activity.songTitle}</span>
          </div>
        )
      case 'SONG_REMOVED':
        return (
          <span>
            <strong>{activity.actorDisplayName}</strong> removeu{' '}
            <span className="activity-song">{activity.songTitle}</span>
          </span>
        )
      case 'ROOM_RENAMED':
        return (
          <span>
            <strong>{activity.actorDisplayName}</strong> mudou o nome da sala para “{activity.detail}”
          </span>
        )
      case 'PLAYBACK_STARTED':
        return (
          <span>
            <span className="activity-song">{activity.songTitle}</span> começou a tocar
          </span>
        )
      case 'PLAYBACK_FINISHED':
        return (
          <span>
            <span className="activity-song">{activity.songTitle}</span> terminou
          </span>
        )
      case 'PLAYBACK_SKIPPED':
        return <span>A música anterior foi pulada</span>
      default:
        return <span>{activity.type}</span>
    }
  }

  return (
    <div className="container room-page stack">
      <header className="room-header">
        <Brand />
        <div className="grow room-header-title">
          <span className="room-header-name">{state?.name ?? `Sala ${codeRef.current}`}</span>
          <span className="room-header-code">{codeRef.current}</span>
        </div>
        <button className="btn btn-sm btn-ghost" onClick={() => void openSummary()} disabled={!roomId}>
          Resumo da sala
        </button>
        <button className="btn btn-sm btn-ghost" onClick={() => setRenaming((v) => !v)}>
          Renomear
        </button>
        <span className="badge badge--live">
          <span className="live-dot" />
          {state?.members.length ?? 0} {state?.members.length === 1 ? 'pessoa' : 'pessoas'}
        </span>
        <button className="btn btn-sm btn-ghost" onClick={copyCode}>
          {copied ? 'Copiado' : 'Copiar código'}
        </button>
        <button className="btn btn-sm btn-ghost" onClick={leave}>
          Sair
        </button>
      </header>

      {renaming && (
        <div className="row">
          <input
            className="input grow"
            placeholder="Nome da sala"
            value={roomName}
            maxLength={40}
            onChange={(event) => setRoomName(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter') {
                void renameRoom()
              }
            }}
          />
          <button className="btn btn-primary" onClick={renameRoom}>
            Salvar
          </button>
        </div>
      )}

      {error && (
        <Notice type="error" onClose={() => setError(null)}>
          {error}
        </Notice>
      )}

      {playerError && (
        <Notice type="error" onClose={() => setPlayerError(false)}>
          Não foi possível reproduzir esta música. Vamos seguir para a próxima.
        </Notice>
      )}

      {summaryOpen && (
        <RoomSummaryDialog summary={summary} loading={summaryLoading} onClose={closeSummary} />
      )}

      {!entered && !error && <div className="muted">Entrando na sala…</div>}

      {entered && state && (
        <div className="room-layout">
          <div className={`panel search-panel ${tabClass('buscar')}`}>
            <h3 className="panel-title">Buscar</h3>
            <div className="row">
              <input
                className="input grow"
                placeholder="Buscar uma música..."
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

            {showResults && (
              <button className="btn btn-sm btn-ghost" onClick={clearSearch}>
                Limpar busca
              </button>
            )}

            {searching && <div className="muted">Buscando…</div>}

            {showResults && (
              <div className="list scroll-area" style={{ maxHeight: 520 }}>
                {results.map((item) => (
                  <div className="list-item" key={item.videoId}>
                    {item.thumbnailUrl ? (
                      <img className="search-thumb" src={item.thumbnailUrl} alt="" />
                    ) : (
                      <span className="search-thumb" aria-hidden="true" />
                    )}
                    <div className="queue-info">
                      <div className="search-title">{item.title}</div>
                      <div className="muted" style={{ fontSize: '0.8rem' }}>
                        {item.channelTitle}
                        {item.channelTitle && item.duration ? ' · ' : ''}
                        {item.duration ? formatDuration(item.duration) : ''}
                      </div>
                    </div>
                    <button
                      className="btn btn-sm btn-primary"
                      aria-label="Adicionar à fila"
                      onClick={() => void addSong(item.videoId)}
                    >
                      +
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className={`panel player-panel ${tabClass('sala')}`}>
            <div className="row" style={{ justifyContent: 'space-between', flexWrap: 'wrap' }}>
              <div className="mode-switch">
                <button
                  className={!isCaixaMode ? 'mode-active' : ''}
                  onClick={() => void changeMode('TODOS_OS_NAVEGADORES')}
                >
                  Todos os dispositivos
                </button>
                <button
                  className={isCaixaMode ? 'mode-active' : ''}
                  onClick={() => void changeMode('CAIXA_DE_MUSICA')}
                >
                  Caixa de música
                </button>
              </div>
            </div>

            {isCaixaMode && (
              <div className="stack" style={{ gap: 6 }}>
                <div className="caixa-state">
                  {caixaAssigned
                    ? isPlayer
                      ? 'Você está reproduzindo.'
                      : 'A caixa está reproduzindo em outro dispositivo.'
                    : 'Nenhum dispositivo assumiu a reprodução.'}
                </div>
                {caixaAssigned && isPlayer ? (
                  <button className="btn btn-sm btn-danger" onClick={releasePlayer}>
                    Liberar caixa
                  </button>
                ) : !caixaAssigned ? (
                  <button className="btn btn-sm" onClick={claimPlayer}>
                    Assumir como caixa
                  </button>
                ) : null}
              </div>
            )}

            {currentPlayback ? (
              <div className="stack" style={{ alignItems: 'stretch' }}>
                <div className="player-cover">
                  <Equalizer active={!isPaused} />
                </div>

                <div className="now-playing">
                  <span className="track-label">Tocando agora</span>
                  <span className="track-title">{currentPlayback.song.title}</span>
                  {currentPlayback.addedByDisplayName && (
                    <span className="track-sub">Adicionada por {currentPlayback.addedByDisplayName}</span>
                  )}
                </div>

                <div
                  className="progress"
                  role="progressbar"
                  aria-valuemin={0}
                  aria-valuemax={durationSeconds ?? 0}
                  aria-valuenow={displayedSeconds}
                  aria-label="Progresso da música"
                >
                  <div className="progress-fill" style={{ width: `${progressPercent}%` }} />
                </div>
                <div className="progress-times">
                  <span>{formatSeconds(displayedSeconds)}</span>
                  <span>{currentPlayback.song.duration ? formatDuration(currentPlayback.song.duration) : ''}</span>
                </div>

                {isPlayer ? (
                  <div className="controls">
                    <YouTubePlayer
                      videoId={currentPlayback.song.youtubeVideoId}
                      playing={!isPaused}
                      startAtSeconds={seekSeconds}
                      syncRequest={syncRequest}
                      volume={volume}
                      muted={muted}
                      onEnded={handleEnded}
                      onError={handlePlaybackError}
                    />
                    <button className="btn btn-primary" onClick={() => void (isPaused ? resume() : pause())}>
                      {isPaused ? 'Retomar' : 'Pausar'}
                    </button>
                    <button className="btn btn-ghost" onClick={() => setMuted((m) => !m)}>
                      {muted ? 'Ativar som' : 'Silenciar'}
                    </button>
                    <input
                      className="volume-slider"
                      type="range"
                      min={0}
                      max={100}
                      value={muted ? 0 : volume}
                      aria-label="Volume"
                      onChange={(event) => {
                        const value = Number(event.target.value)
                        setVolume(value)
                        if (value > 0) {
                          setMuted(false)
                        }
                      }}
                    />
                  </div>
                ) : isCaixaMode && !caixaAssigned ? (
                  <div className="muted">Nenhum dispositivo reproduzindo. Assuma a caixa para tocar o áudio.</div>
                ) : (
                  <div className="muted">Aguarde — somente a caixa desta sala reproduz o áudio.</div>
                )}
              </div>
            ) : (
              <div className="muted">Nada tocando no momento.</div>
            )}

            {currentPlayback && state.skipVote && (
              <div className="skip-area">
                <div className="skip-title">Pular esta música</div>
                <div className="vote-bar">
                  <div className="vote-fill" style={{ width: `${votePercent}%` }} />
                </div>
                <div className="row" style={{ justifyContent: 'space-between' }}>
                  <span className="muted">
                    {state.skipVote.votes} de {state.skipVote.requiredVotes} votos necessários
                  </span>
                  <button
                    className="btn"
                    onClick={voteSkip}
                    disabled={state.skipVote.currentUserVoted}
                  >
                    {state.skipVote.currentUserVoted ? 'Voto registrado' : 'Votar para pular'}
                  </button>
                </div>
              </div>
            )}
          </div>

          <div className={`panel people-panel ${tabClass('pessoas')}`}>
            <h3 className="panel-title">Na sala</h3>
            {state.members.length === 0 && <div className="muted">Ninguém na sala.</div>}
            {state.members.map((member) => (
              <div className="participant" key={member.userId}>
                <Avatar
                  userId={member.userId}
                  displayName={member.displayName}
                  avatarUrl={member.avatarUrl}
                  size="sm"
                />
                <div className="participant-name">{member.displayName}</div>
                <span className="participant-count">{member.waitingCount}/8</span>
              </div>
            ))}
          </div>

          <div className={`panel activity-panel ${tabClass('atividade')}`}>
            <h3 className="panel-title">Acontecendo agora</h3>
            <div className="scroll-area" style={{ maxHeight: 520 }}>
              {activities.length === 0 && <div className="muted">Nenhuma atividade ainda.</div>}
              {activities.map((activity) => (
                <div className="activity-item" key={activity.id}>
                  <span className="activity-icon" aria-hidden="true">
                    {activityGlyph(activity.type)}
                  </span>
                  <div className="activity-body">
                    <div className="activity-line">{activityLine(activity)}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className={`panel history-panel ${tabClass('atividade')}`}>
            <h3 className="panel-title">Histórico</h3>
            <div className="list scroll-area" style={{ maxHeight: 400 }}>
              {history.length === 0 && <div className="muted">Nenhuma reprodução ainda.</div>}
              {history.map((item) => (
                <div className="queue-item" key={item.playbackId}>
                  {item.addedBy && (
                    <Avatar
                      userId={item.addedBy.id}
                      displayName={item.addedBy.name}
                      avatarUrl={item.addedBy.avatarUrl}
                      size="sm"
                    />
                  )}
                  {item.song.thumbnailUrl && (
                    <img className="queue-thumb" src={item.song.thumbnailUrl} alt="" />
                  )}
                  <div className="queue-info">
                    <div className="ellipsis">{item.song.title}</div>
                    <div className="muted" style={{ fontSize: '0.8rem' }}>
                      {item.status} · {formatDuration(item.song.duration)}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className={`panel queue-panel ${tabClass('sala')}`}>
            <h3 className="panel-title">
              Próximas <span className="muted">· {state.queue.length}</span>
            </h3>
            <div className="list scroll-area" style={{ maxHeight: 400 }}>
              {state.queue.length === 0 && <div className="muted">A fila está vazia.</div>}
              {state.queue.map((item) => (
                <div className="queue-item" key={item.queueItemId}>
                  {item.song.thumbnailUrl && (
                    <img className="queue-thumb" src={item.song.thumbnailUrl} alt={item.song.title} />
                  )}
                  <div className="queue-info">
                    <div className="ellipsis">{item.song.title}</div>
                    <div className="muted" style={{ fontSize: '0.8rem' }}>
                      {formatDuration(item.song.duration)}
                      {item.status === 'PLAYING' ? ' · tocando' : ''}
                      {item.source === 'AUTO_DJ' && (
                        <span className="tag-autodj"> Auto-DJ</span>
                      )}
                    </div>
                  </div>
                  {item.status === 'WAITING' && (
                    <button className="btn btn-sm btn-ghost" onClick={() => void removeItem(item.queueItemId)}>
                      Remover
                    </button>
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      <nav className="mobile-tabbar" aria-label="Navegação da sala">
        <div className="mobile-tabbar-inner">
          <button
            className={`mobile-tab${mobileTab === 'sala' ? ' mobile-tab--active' : ''}`}
            onClick={() => setMobileTab('sala')}
          >
            Sala
          </button>
          <button
            className={`mobile-tab${mobileTab === 'buscar' ? ' mobile-tab--active' : ''}`}
            onClick={() => setMobileTab('buscar')}
          >
            Buscar
          </button>
          <button
            className={`mobile-tab${mobileTab === 'pessoas' ? ' mobile-tab--active' : ''}`}
            onClick={() => setMobileTab('pessoas')}
          >
            Pessoas
          </button>
          <button
            className={`mobile-tab${mobileTab === 'atividade' ? ' mobile-tab--active' : ''}`}
            onClick={() => setMobileTab('atividade')}
          >
            Atividade
          </button>
        </div>
      </nav>
    </div>
  )
}
