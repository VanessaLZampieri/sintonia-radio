export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

export function extractErrorMessage(body: unknown, status: number): string {
  if (isRecord(body) && typeof body.message === 'string' && body.message.trim() !== '') {
    return body.message
  }
  return fallbackMessageForStatus(status)
}

export function fallbackMessageForStatus(status: number): string {
  if (status === 0) {
    return 'Falha de comunicação com o servidor. Verifique sua conexão.'
  }
  if (status >= 500) {
    return 'O servidor encontrou um problema. Tente novamente em instantes.'
  }
  if (status === 401) {
    return 'Sua sessão expirou. Entre novamente.'
  }
  if (status === 403) {
    return 'Você não tem permissão para fazer esta ação.'
  }
  if (status === 404) {
    return 'Não encontrado.'
  }
  return 'Não foi possível concluir a ação. Tente novamente.'
}
