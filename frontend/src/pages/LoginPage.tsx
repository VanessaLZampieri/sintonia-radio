import { Navigate } from 'react-router-dom'
import { useAuth } from '../auth'

export function LoginPage() {
  const { me, loading } = useAuth()

  if (loading) {
    return <div className="screen-center">Carregando…</div>
  }

  if (me) {
    return <Navigate to="/" replace />
  }

  return (
    <div className="screen-center">
      <div className="card stack" style={{ minWidth: 340, textAlign: 'center' }}>
        <h1 style={{ margin: 0 }}>Sintonia</h1>
        <p className="muted" style={{ margin: 0 }}>
          Uma rádio compartilhada.
        </p>
        <a className="btn btn-primary" href="/oauth2/authorization/google">
          Entrar com Google
        </a>
      </div>
    </div>
  )
}
