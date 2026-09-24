import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getOrCreateClientSessionId } from './session'

describe('getOrCreateClientSessionId', () => {
  beforeEach(() => {
    vi.unstubAllGlobals()
  })

  it('gera um UUID, persiste e devolve o mesmo valor nas chamadas seguintes', () => {
    const store = new Map<string, string>()
    vi.stubGlobal('sessionStorage', {
      getItem: (key: string) => store.get(key) ?? null,
      setItem: (key: string, value: string) => {
        store.set(key, value)
      },
    })

    const first = getOrCreateClientSessionId()
    const second = getOrCreateClientSessionId()

    expect(first).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/)
    expect(second).toBe(first)
    expect(store.get('sintonia.clientSessionId')).toBe(first)
  })

  it('devolve o valor persistido quando já existe', () => {
    vi.stubGlobal('sessionStorage', {
      getItem: () => 'id-persistido',
      setItem: vi.fn(),
    })

    expect(getOrCreateClientSessionId()).toBe('id-persistido')
  })

  it('gera outro ID quando uma nova aba herda o sessionStorage', () => {
    const store = new Map([['sintonia.clientSessionId', 'id-copiado']])
    const browserWindow = { name: '' }
    vi.stubGlobal('window', browserWindow)
    vi.stubGlobal('sessionStorage', {
      getItem: (key: string) => store.get(key) ?? null,
      setItem: (key: string, value: string) => store.set(key, value),
    })

    const id = getOrCreateClientSessionId()

    expect(id).not.toBe('id-copiado')
    expect(browserWindow.name).toBe(`sintonia-client:${id}`)
    expect(store.get('sintonia.clientSessionId')).toBe(id)
  })
})
