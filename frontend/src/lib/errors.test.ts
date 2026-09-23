import { describe, expect, it } from 'vitest'
import { ApiError, extractErrorMessage, fallbackMessageForStatus } from './errors'

describe('extractErrorMessage', () => {
  it('usa a mensagem do corpo quando presente', () => {
    expect(extractErrorMessage({ message: 'Esta sala foi encerrada.' }, 409)).toBe(
      'Esta sala foi encerrada.',
    )
  })

  it('ignora mensagem vazia', () => {
    expect(extractErrorMessage({ message: '  ' }, 409)).toBe(fallbackMessageForStatus(409))
  })

  it('usa fallback quando o corpo é null', () => {
    expect(extractErrorMessage(null, 500)).toBe(fallbackMessageForStatus(500))
  })

  it('usa fallback quando o corpo não é um objeto', () => {
    expect(extractErrorMessage('texto solto', 500)).toBe(fallbackMessageForStatus(500))
  })

  it('usa fallback quando não existe campo message', () => {
    expect(extractErrorMessage({ error: 'Bad Request' }, 400)).toBe(fallbackMessageForStatus(400))
  })
})

describe('fallbackMessageForStatus', () => {
  it('trata falha de rede (status 0)', () => {
    expect(fallbackMessageForStatus(0)).toBe(
      'Falha de comunicação com o servidor. Verifique sua conexão.',
    )
  })

  it('trata erro de servidor (5xx)', () => {
    expect(fallbackMessageForStatus(500)).toBe(
      'O servidor encontrou um problema. Tente novamente em instantes.',
    )
  })

  it('trata não encontrado (404)', () => {
    expect(fallbackMessageForStatus(404)).toBe('Não encontrado.')
  })

  it('trata sem permissão (403)', () => {
    expect(fallbackMessageForStatus(403)).toBe('Você não tem permissão para fazer esta ação.')
  })

  it('trata sessão expirada (401)', () => {
    expect(fallbackMessageForStatus(401)).toBe('Sua sessão expirou. Entre novamente.')
  })

  it('trata erro genérico', () => {
    expect(fallbackMessageForStatus(418)).toBe('Não foi possível concluir a ação. Tente novamente.')
  })
})

describe('ApiError', () => {
  it('preserva o status HTTP e a mensagem', () => {
    const error = new ApiError(409, 'Esta sala foi encerrada.')

    expect(error.status).toBe(409)
    expect(error.message).toBe('Esta sala foi encerrada.')
    expect(error).toBeInstanceOf(Error)
  })
})
