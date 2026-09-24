import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, ApiError, logout } from '../api'
import { useAuth } from '../auth'
import { Avatar } from '../components/Avatar'
import { Brand } from '../components/Brand'
import { notifyFailure, notifySuccess } from '../lib/notify'
import { getOrCreateClientSessionId } from '../lib/session'
import type { ActiveRoom, UserRoom } from '../types'

function NoteIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path
        d="M9 18V6l10-2v11"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle cx="6" cy="18" r="3" fill="currentColor" />
      <circle cx="16" cy="15" r="3" fill="currentColor" />
    </svg>
  )
}

export function HomePage() {
  const { me, reload } = useAuth()
  const navigate = useNavigate()
  const [code, setCode] = useState('')
  const [roomName, setRoomName] = useState('')
  const [creating, setCreating] = useState(false)
  const [editingName, setEditingName] = useState(false)
  const [displayName, setDisplayName] = useState('')
  const [savingName, setSavingName] = useState(false)
  const [busy, setBusy] = useState(false)
  const [activeRooms, setActiveRooms] = useState<ActiveRoom[]>([])
  const [myRooms, setMyRooms] = useState<UserRoom[]>([])

  useEffect(() => {
    if (me?.displayName) {
      setDisplayName(me.displayName)
    }
  }, [me?.displayName])

  useEffect(() => {
    void loadRooms()
  }, [])

  const loadRooms = async () => {
    try {
      const [active, mine] = await Promise.all([api.listActiveRooms(), api.myRooms()])
      setActiveRooms(active)
      setMyRooms(mine)
    } catch {
      // Ignora falha ao carregar as listas; o usuário ainda pode criar/entrar por código.
    }
  }

  const saveDisplayName = async () => {
    setSavingName(true)
    try {
      await api.updateDisplayName(displayName)
      await reload()
      setEditingName(false)
      notifySuccess('Nome de exibição atualizado.')
    } catch (err) {
      notifyFailure(err instanceof ApiError ? err.message : 'Não foi possível atualizar o nome.')
    } finally {
      setSavingName(false)
    }
  }

  const createRoom = async () => {
    const name = roomName.trim()
    if (name.length < 3) {
      notifyFailure('Digite um nome para a sala (mínimo 3 caracteres).')
      return
    }
    setBusy(true)
    try {
      const room = await api.createRoom(name)
      navigate(`/room/${room.code}`)
    } catch (err) {
      notifyFailure(err instanceof ApiError ? err.message : 'Não foi possível criar a sala.')
    } finally {
      setBusy(false)
    }
  }

  const enterByCode = async (rawCode: string) => {
    const trimmed = rawCode.trim()
    if (!trimmed) {
      notifyFailure('Digite o código da sala.')
      return
    }
    setBusy(true)
    try {
      await api.enterRoom(trimmed, getOrCreateClientSessionId())
      navigate(`/room/${trimmed.toUpperCase()}`)
    } catch (err) {
      notifyFailure(err instanceof ApiError ? err.message : 'Não foi possível entrar na sala.')
    } finally {
      setBusy(false)
    }
  }

  const doLogout = async () => {
    await logout()
    await reload()
    navigate('/login')
  }

  const visibleMyRooms = myRooms.filter((room) => room.status === 'ACTIVE')

  return (
    <div className="container stack">
      <header className="home-header">
        <Brand />

        <div className="row" style={{ justifyContent: 'flex-end' }}>
          <div className="user-chip">
            <Avatar
              userId={me?.id ?? 0}
              displayName={me?.displayName ?? ''}
              avatarUrl={me?.avatarUrl ?? null}
            />
            <div className="stack" style={{ gap: 2 }}>
              <span>{me?.displayName}</span>
              <span className="muted" style={{ fontSize: '0.8rem' }}>{me?.name}</span>
            </div>
            <button className="btn btn-sm btn-ghost" onClick={() => setEditingName((v) => !v)}>
              Editar
            </button>
            <button className="btn btn-sm btn-ghost" onClick={doLogout}>
              Sair
            </button>
          </div>
        </div>

        {editingName && (
          <div className="row" style={{ width: '100%' }}>
            <input
              className="input grow"
              placeholder="Seu nome de exibição"
              value={displayName}
              maxLength={20}
              onChange={(event) => setDisplayName(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter') {
                  void saveDisplayName()
                }
              }}
            />
            <button className="btn" onClick={saveDisplayName} disabled={savingName}>
              {savingName ? 'Salvando…' : 'Salvar'}
            </button>
          </div>
        )}
      </header>

      <div className="create-bar">
        {creating ? (
          <>
            <input
              className="input"
              placeholder="Nome da sala"
              value={roomName}
              maxLength={40}
              autoFocus
              onChange={(event) => setRoomName(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter') {
                  void createRoom()
                }
              }}
            />
            <button className="btn btn-primary" onClick={createRoom} disabled={busy}>
              Criar
            </button>
            <button className="btn btn-ghost" onClick={() => setCreating(false)}>
              Cancelar
            </button>
          </>
        ) : (
          <button className="btn btn-primary" onClick={() => setCreating(true)}>
            + Criar uma sala
          </button>
        )}

        <input
          className="input"
          style={{ maxWidth: 240 }}
          placeholder="Entrar com código"
          value={code}
          onChange={(event) => setCode(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              void enterByCode(code)
            }
          }}
        />
        <button className="btn" onClick={() => void enterByCode(code)} disabled={busy}>
          Entrar
        </button>
      </div>

      <section className="section">
        <h2 className="section-title">Salas no ar</h2>
        {activeRooms.length === 0 ? (
          <div className="muted">Nenhuma sala no ar agora.</div>
        ) : (
          <div className="room-grid">
            {activeRooms.map((room) => (
              <div className="room-card" key={room.roomId}>
                <div className="room-card-head">
                  <div className="room-card-title">{room.name}</div>
                  <span className="badge badge--live">
                    <span className="live-dot" />
                    AO VIVO
                  </span>
                </div>
                <div className="room-card-code">{room.code}</div>
                {room.nowPlaying ? (
                  <div className="stack" style={{ gap: 2 }}>
                    <div className="now-line">
                      <NoteIcon />
                      <span className="now-line-text">{room.nowPlaying.title}</span>
                    </div>
                    {room.nowPlaying.addedBy && (
                      <div className="added-by muted">
                        <Avatar
                          userId={room.nowPlaying.addedBy.userId}
                          displayName={room.nowPlaying.addedBy.displayName}
                          avatarUrl={room.nowPlaying.addedBy.avatarUrl}
                          size="sm"
                        />
                        <span>adicionada por {room.nowPlaying.addedBy.displayName}</span>
                      </div>
                    )}
                  </div>
                ) : (
                  <div className="muted">Sem música no momento</div>
                )}
                <div className="room-card-meta">
                  <span>
                    {room.participantCount} {room.participantCount === 1 ? 'pessoa' : 'pessoas'}
                  </span>
                  <span>·</span>
                  <span>{room.waitingCount} na fila</span>
                </div>
                <button className="btn" onClick={() => void enterByCode(room.code)} disabled={busy}>
                  Entrar
                </button>
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="section">
        <h2 className="section-title">Suas salas</h2>
        {visibleMyRooms.length === 0 ? (
          <div className="muted">Você ainda não participou de nenhuma sala.</div>
        ) : (
          <div className="room-grid">
            {visibleMyRooms.map((room) => {
              const live = room.participantCount > 0
              return (
                <div className="room-card" key={room.roomId}>
                  <div className="room-card-head">
                    <div className="room-card-title">{room.name}</div>
                    {live ? (
                      <span className="badge badge--live">
                        <span className="live-dot" />
                        AO VIVO
                      </span>
                    ) : (
                      <span className="badge">VAZIA</span>
                    )}
                  </div>
                  <div className="room-card-code">{room.code}</div>
                  <div className="room-card-meta">
                    <span>
                      {room.participantCount} {room.participantCount === 1 ? 'pessoa' : 'pessoas'}
                    </span>
                  </div>
                  {room.canEnter ? (
                    <button
                      className="btn btn-primary"
                      onClick={() => void enterByCode(room.code)}
                      disabled={busy}
                    >
                      Entrar
                    </button>
                  ) : (
                    <div className="muted">Cheia</div>
                  )}
                </div>
              )
            })}
          </div>
        )}
      </section>
    </div>
  )
}
