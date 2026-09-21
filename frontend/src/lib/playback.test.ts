import { describe, expect, it } from 'vitest'
import {
  clampSeekSeconds,
  formatDuration,
  parseDurationSeconds,
  resumeTargetSeconds,
  shouldPauseLocally,
  shouldPlay,
} from './playback'

describe('shouldPlay', () => {
  it('é true em TODOS_OS_NAVEGADORES', () => {
    expect(shouldPlay('TODOS_OS_NAVEGADORES', null, 'minha-sessao')).toBe(true)
  })

  it('é true em CAIXA_DE_MUSICA quando a sessão é a do player', () => {
    expect(shouldPlay('CAIXA_DE_MUSICA', 'minha-sessao', 'minha-sessao')).toBe(true)
  })

  it('é false em CAIXA_DE_MUSICA quando a sessão é de outro cliente', () => {
    expect(shouldPlay('CAIXA_DE_MUSICA', 'outra-sessao', 'minha-sessao')).toBe(false)
  })

  it('é false em CAIXA_DE_MUSICA quando não há player', () => {
    expect(shouldPlay('CAIXA_DE_MUSICA', null, 'minha-sessao')).toBe(false)
  })
})

describe('shouldPauseLocally', () => {
  it('é true em TODOS_OS_NAVEGADORES', () => {
    expect(shouldPauseLocally('TODOS_OS_NAVEGADORES')).toBe(true)
  })

  it('é false em CAIXA_DE_MUSICA', () => {
    expect(shouldPauseLocally('CAIXA_DE_MUSICA')).toBe(false)
  })
})

describe('formatDuration', () => {
  it('formata minutos e segundos', () => {
    expect(formatDuration('PT3M33S')).toBe('3:33')
  })

  it('formata horas, minutos e segundos', () => {
    expect(formatDuration('PT1H2M3S')).toBe('1:02:03')
  })

  it('devolve a entrada quando não é ISO-8601', () => {
    expect(formatDuration('desconhecido')).toBe('desconhecido')
  })
})

describe('parseDurationSeconds', () => {
  it('converte PT3M33S em 213 segundos', () => {
    expect(parseDurationSeconds('PT3M33S')).toBe(213)
  })

  it('converte horas em segundos', () => {
    expect(parseDurationSeconds('PT1H0M0S')).toBe(3600)
  })

  it('retorna null para valor inválido', () => {
    expect(parseDurationSeconds('abc')).toBeNull()
  })
})

describe('clampSeekSeconds', () => {
  it('nunca retorna posição negativa', () => {
    expect(clampSeekSeconds(-5, 100)).toBe(0)
  })

  it('limita ao final (duration - 1)', () => {
    expect(clampSeekSeconds(120, 100)).toBe(99)
  })

  it('mantém posição válida', () => {
    expect(clampSeekSeconds(45, 100)).toBe(45)
  })

  it('aceita duração desconhecida (null)', () => {
    expect(clampSeekSeconds(45, null)).toBe(45)
  })
})

describe('resumeTargetSeconds', () => {
  it('usa a posição atual quando está dentro da duração', () => {
    expect(resumeTargetSeconds(95, 'PT3M33S')).toBe(95)
  })

  it('limita ao final da faixa (duration - 1)', () => {
    expect(resumeTargetSeconds(300, 'PT3M33S')).toBe(212)
  })

  it('nunca retorna posição negativa', () => {
    expect(resumeTargetSeconds(-5, 'PT3M33S')).toBe(0)
  })

  it('mantém a posição quando a duração não é ISO-8601 válida', () => {
    expect(resumeTargetSeconds(45, 'desconhecido')).toBe(45)
  })
})
