import { Navigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../auth'
import { Notice } from '../components/Notice'
import { LogoMark } from '../components/LogoMark'

function GoogleIcon() {
  return (
    <svg viewBox="0 0 18 18" aria-hidden="true" focusable="false">
      <path fill="#4285F4" d="M17.64 9.205c0-.638-.057-1.252-.164-1.841H9v3.482h4.844a4.14 4.14 0 0 1-1.797 2.715v2.259h2.909c1.702-1.567 2.684-3.875 2.684-6.615Z" />
      <path fill="#34A853" d="M9 18c2.43 0 4.468-.806 5.956-2.18l-2.91-2.259c-.805.54-1.835.859-3.046.859-2.344 0-4.328-1.585-5.037-3.715H.956v2.332A9 9 0 0 0 9 18Z" />
      <path fill="#FBBC05" d="M3.963 10.705A5.41 5.41 0 0 1 3.68 9c0-.592.102-1.168.283-1.705V4.963H.956A9 9 0 0 0 0 9c0 1.45.347 2.824.956 4.037l3.007-2.332Z" />
      <path fill="#EA4335" d="M9 3.58c1.322 0 2.508.454 3.441 1.346l2.581-2.581C13.464.893 11.427 0 9 0A9 9 0 0 0 .956 4.963l3.007 2.332C4.672 5.165 6.656 3.58 9 3.58Z" />
    </svg>
  )
}

export function LoginPage() {
  const { me, loading } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const oauthFailed = searchParams.has('error')

  if (loading) {
    return (
      <div className="login">
        <div className="muted">Carregando…</div>
      </div>
    )
  }

  if (me) {
    return <Navigate to="/" replace />
  }

  return (
    <div className="login">
      <main className="login-content">
        <div className="login-brand" aria-label="Sintonia, rádio compartilhada">
          <LogoMark className="login-wave" />
          <span className="login-brand-name">sintonia</span>
          <span className="login-brand-tag">RÁDIO COMPARTILHADA</span>
        </div>

        <p className="login-slogan">
          Boa música toca melhor
          <br />
          em boa companhia.
        </p>

        <section className="login-auth-card">
          {oauthFailed && (
            <Notice type="error" onClose={() => setSearchParams({}, { replace: true })}>
              Não foi possível entrar com o Google. Tente novamente.
            </Notice>
          )}

          <a className="btn btn-primary login-google" href="/oauth2/authorization/google">
            <GoogleIcon />
            <span>Continuar com Google</span>
            <span aria-hidden="true" />
          </a>

          <p className="login-hint">
            Conecte-se, crie uma sala e
            <br />
            viva a música junto com outras pessoas.
          </p>
        </section>
      </main>
    </div>
  )
}
