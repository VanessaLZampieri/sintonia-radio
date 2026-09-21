import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, ApiError, logout } from '../api'
import { useAuth } from '../auth'

export function HomePage() {
  const { me, reload } = useAuth()
  const navigate = useNavigate()
  const [code, setCode] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const createRoom = async () => {
    setBusy(true)
    setError(null)
    try {
      const room = await api.createRoom()
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
          {me?.avatarUrl ? <img src={me.avatarUrl} alt={me.name} /> : (me?.name?.[0] ?? '?')}
        </div>
        <div className="grow">
          <div>{me?.name}</div>
          <div className="muted" style={{ fontSize: '0.85rem' }}>
            {me?.email}
          </div>
        </div>
        <button className="btn btn-sm" onClick={doLogout}>
          Sair
        </button>
      </header>

      <div className="grid-2">
        <div className="card stack">
          <h2 style={{ margin: 0 }}>Criar sala</h2>
          <p className="muted" style={{ margin: 0 }}>
            Crie uma sala e compartilhe o código com quem quiser.
          </p>
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
