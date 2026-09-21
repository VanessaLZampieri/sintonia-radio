import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { useAuth } from './auth'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { RoomPage } from './pages/RoomPage'

function RequireAuth({ children }: { children: JSX.Element }) {
  const { me, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return <div className="screen-center">Carregando…</div>
  }

  if (!me) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  return children
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/"
        element={
          <RequireAuth>
            <HomePage />
          </RequireAuth>
        }
      />
      <Route
        path="/room/:code"
        element={
          <RequireAuth>
            <RoomPage />
          </RequireAuth>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
