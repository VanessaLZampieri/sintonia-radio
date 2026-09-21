import { describe, expect, it } from 'vitest'
import { csrfHeaders, parseCookie, requiresCsrf } from './csrf'

describe('parseCookie', () => {
  it('lê um cookie pelo nome no meio da string', () => {
    expect(parseCookie('XSRF-TOKEN', 'JSESSIONID=abc; XSRF-TOKEN=token123')).toBe('token123')
  })

  it('lê um cookie quando ele é o único', () => {
    expect(parseCookie('XSRF-TOKEN', 'XSRF-TOKEN=token123')).toBe('token123')
  })

  it('retorna null quando o cookie não existe', () => {
    expect(parseCookie('XSRF-TOKEN', 'JSESSIONID=abc')).toBeNull()
  })

  it('decodifica valor URL-encoded', () => {
    expect(parseCookie('XSRF-TOKEN', 'XSRF-TOKEN=a%2Bb%2Fc')).toBe('a+b/c')
  })

  it('retorna o valor cru quando a decodificação falha', () => {
    expect(parseCookie('XSRF-TOKEN', 'XSRF-TOKEN=abc%zz')).toBe('abc%zz')
  })
})

describe('requiresCsrf', () => {
  it('é true para POST', () => {
    expect(requiresCsrf('POST')).toBe(true)
  })

  it('é true para PUT', () => {
    expect(requiresCsrf('PUT')).toBe(true)
  })

  it('é true para DELETE', () => {
    expect(requiresCsrf('DELETE')).toBe(true)
  })

  it('é true para PATCH', () => {
    expect(requiresCsrf('PATCH')).toBe(true)
  })

  it('é false para GET', () => {
    expect(requiresCsrf('GET')).toBe(false)
  })

  it('é false para HEAD', () => {
    expect(requiresCsrf('HEAD')).toBe(false)
  })

  it('é false para OPTIONS', () => {
    expect(requiresCsrf('OPTIONS')).toBe(false)
  })
})

describe('csrfHeaders', () => {
  it('envia X-XSRF-TOKEN em POST', () => {
    expect(csrfHeaders('POST', 'XSRF-TOKEN=tok')).toEqual({ 'X-XSRF-TOKEN': 'tok' })
  })

  it('envia X-XSRF-TOKEN em PUT', () => {
    expect(csrfHeaders('PUT', 'XSRF-TOKEN=tok')).toEqual({ 'X-XSRF-TOKEN': 'tok' })
  })

  it('envia X-XSRF-TOKEN em DELETE', () => {
    expect(csrfHeaders('DELETE', 'XSRF-TOKEN=tok')).toEqual({ 'X-XSRF-TOKEN': 'tok' })
  })

  it('não envia header em GET', () => {
    expect(csrfHeaders('GET', 'XSRF-TOKEN=tok')).toEqual({})
  })

  it('não envia header quando o cookie não existe', () => {
    expect(csrfHeaders('POST', 'JSESSIONID=abc')).toEqual({})
  })

  it('decodifica o token URL-encoded antes de enviar', () => {
    expect(csrfHeaders('POST', 'XSRF-TOKEN=a%2Bb')).toEqual({ 'X-XSRF-TOKEN': 'a+b' })
  })
})
