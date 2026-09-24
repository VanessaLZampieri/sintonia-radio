import { Component, type ReactNode } from 'react'

interface Props {
  children: ReactNode
}

interface State {
  error: Error | null
}

export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null }

  static getDerivedStateFromError(error: Error): State {
    return { error }
  }

  componentDidCatch(error: Error, info: unknown) {
    console.error('Erro de renderização:', error, info)
  }

  render() {
    if (this.state.error) {
      return (
        <div className="screen-center">
          <div className="card stack" style={{ maxWidth: 420, textAlign: 'center' }}>
            <h2 style={{ margin: 0 }}>Algo deu errado</h2>
            <p className="muted" style={{ margin: 0 }}>
              Ocorreu um problema inesperado ao renderizar esta tela.
            </p>
            <button className="btn btn-primary" onClick={() => this.setState({ error: null })}>
              Tentar novamente
            </button>
          </div>
        </div>
      )
    }
    return this.props.children
  }
}
