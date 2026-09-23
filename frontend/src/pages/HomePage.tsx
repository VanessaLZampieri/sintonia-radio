import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, ApiError, logout } from '../api'
import { useAuth } from '../auth'
import { Notice } from '../components/Notice'
import type { ActiveRoom, UserRoom } from '../types'

export function HomePage() {
  const { me, reload } = useAuth()
  const navigate = useNavigate()
  const [code, setCode] = useState('')
  const [roomName, setRoomName] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [savingName, setSavingName] = useState(false)
  const [nameMessage, setNameMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [activeRooms, setActiveRooms] = useState<ActiveRoom[]>([])
  const [myRooms, setMyRooms] = useState<UserRoom[]>([])

  const loadRooms = async () => {
    try {
      const [active, mine] = await Promise.all([api.listActiveRooms(), api.myRooms()])
      setActiveRooms(active)
      setMyRooms(mine)
    } catch {
      // Ignora falha ao carregar as listas; o usuário ainda pode criar/entrar por código.
    }
  }

  useEffect(() => {
    void loadRooms()
  }, [])

  useEffect(() => {
    if (me?.displayName) {
      setDisplayName(me.displayName)
    }
  }, [me?.displayName])

  const saveDisplayName = async () => {
    setSavingName(true)
    setNameMessage(null)
    try {
      await api.updateDisplayName(displayName)
      await reload()
      setNameMessage('Nome de exibição atualizado.')
    } catch (err) {
      setNameMessage(err instanceof ApiError ? err.message : 'Não foi possível atualizar o nome.')
    } finally {
      setSavingName(false)
    }
  }

  const createRoom = async () => {
    const name = roomName.trim()
    if (name.length < 3) {
      setError('Digite um nome para a sala (mínimo 3 caracteres).')
      return
    }
    setBusy(true)
    setError(null)
    try {
      const room = await api.createRoom(name)
      navigate(`/room/${room.code}`)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível criar a sala.')
    } finally {
      setBusy(false)
    }
  }

  const enterByCode = async (rawCode: string) => {
    const trimmed = rawCode.trim()
    if (!trimmed) {
      setError('Digite o código da sala.')
      return
    }
    setBusy(true)
    setError(null)
    try {
      await api.enterRoom(trimmed)
      navigate(`/room/${trimmed.toUpperCase()}`)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível entrar na sala.')
    } finally {
      setBusy(false)
    }
  }

  const doLogout = async () => {
    await logout()
    await reload()
    navigate('/login')
  }

  return (
    <div className="container stack">
      <header className="row">
        <div className="avatar">
          {me?.avatarUrl ? (
            <img src={me.avatarUrl} alt={me.displayName} />
          ) : (
            (me?.displayName?.[0] ?? me?.name?.[0] ?? '?')
          )}
        </div>
        <div className="grow">
          <div>{me?.displayName}</div>
          <div className="muted" style={{ fontSize: '0.85rem' }}>
            {me?.name}
          </div>
          <div className="muted" style={{ fontSize: '0.85rem' }}>
            {me?.email}
          </div>
        </div>
        <button className="btn btn-sm" onClick={doLogout}>
          Sair
        </button>
      </header>

      <div className="card stack">
        <h2 style={{ margin: 0 }}>Nome de exibição</h2>
        <p className="muted" style={{ margin: 0 }}>
          Este é o nome que as outras pessoas veem nas salas.
        </p>
        <div className="row">
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
        {nameMessage && (
          <Notice type="success" onClose={() => setNameMessage(null)}>
            {nameMessage}
          </Notice>
        )}
      </div>

      <div className="grid-2">
        <div className="card stack">
          <h2 style={{ margin: 0 }}>Criar sala</h2>
          <p className="muted" style={{ margin: 0 }}>
            Dê um nome à sala e compartilhe o código com quem quiser.
          </p>
          <input
            className="input"
            placeholder="Nome da sala"
            value={roomName}
            maxLength={40}
            onChange={(event) => setRoomName(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter') {
                void createRoom()
              }
            }}
          />
          <button className="btn btn-primary" onClick={createRoom} disabled={busy}>
            Criar nova sala
          </button>
        </div>

        <div className="card stack">
          <h2 style={{ margin: 0 }}>Entrar em uma sala</h2>
          <input
            className="input"
            placeholder="Código da sala"
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
      </div>

      <div className="card stack">
        <h2 style={{ margin: 0 }}>Salas no ar</h2>
        {activeRooms.length === 0 && <div className="muted">Nenhuma sala no ar agora.</div>}
        <div className="list">
          {activeRooms.map((room) => (
            <div className="list-item" key={room.roomId}>
              <div className="grow">
                <div className="ellipsis">{room.name}</div>
                <div className="muted" style={{ fontSize: '0.85rem' }}>
                  {room.code} · {room.participantCount} participante(s) · {room.waitingCount} na fila
                  {room.nowPlaying ? ` · Tocando: ${room.nowPlaying.title}` : ''}
                </div>
              </div>
              <button className="btn btn-sm" onClick={() => void enterByCode(room.code)} disabled={busy}>
                Entrar
              </button>
            </div>
          ))}
        </div>
      </div>

      <div className="card stack">
        <h2 style={{ margin: 0 }}>Suas salas</h2>
        {myRooms.length === 0 && <div className="muted">Você ainda não participou de nenhuma sala.</div>}
        <div className="list">
          {myRooms.map((room) => (
            <div className="list-item" key={room.roomId}>
              <div className="grow">
                <div className="ellipsis">{room.name}</div>
                <div className="muted" style={{ fontSize: '0.85rem' }}>
                  {room.code} · {room.participantCount} participante(s)
                  {room.status === 'CLOSED' ? ' · Encerrada' : ''}
                </div>
              </div>
              {room.canEnter ? (
                <button className="btn btn-sm" onClick={() => void enterByCode(room.code)} disabled={busy}>
                  Entrar
                </button>
              ) : room.status === 'CLOSED' ? (
                <span className="muted">Encerrada</span>
              ) : (
                <span className="muted">Cheia</span>
              )}
            </div>
          ))}
        </div>
      </div>

      {error && (
        <Notice type="error" onClose={() => setError(null)}>
          {error}
        </Notice>
      )}
    </div>
  )
}
