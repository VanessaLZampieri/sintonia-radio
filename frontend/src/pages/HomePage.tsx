import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, ApiError, logout } from '../api'
import { useAuth } from '../auth'

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

  const enterRoom = async () => {
    const trimmed = code.trim()
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
        {nameMessage && <div className="muted">{nameMessage}</div>}
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
                void enterRoom()
              }
            }}
          />
          <button className="btn" onClick={enterRoom} disabled={busy}>
            Entrar
          </button>
        </div>
      </div>

      {error && <div className="error-banner">{error}</div>}
    </div>
  )
}
