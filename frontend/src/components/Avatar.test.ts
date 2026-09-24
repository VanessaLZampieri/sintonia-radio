// @vitest-environment jsdom

import { act, createElement } from 'react'
import { createRoot } from 'react-dom/client'
import { afterEach, describe, expect, it } from 'vitest'
import { Avatar, avatarVariant } from './Avatar'

(globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }).IS_REACT_ACT_ENVIRONMENT = true

describe('avatarVariant', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('mantém a mesma variante para o mesmo usuário', () => {
    expect(avatarVariant(4821)).toBe(avatarVariant(4821))
  })

  it('distribui IDs estáveis pelas oito variantes disponíveis', () => {
    expect(new Set(Array.from({ length: 8 }, (_, id) => avatarVariant(id))).size).toBe(8)
  })

  it('usa o avatar Sintonia quando a foto externa falha', () => {
    const host = document.createElement('div')
    document.body.appendChild(host)
    const root = createRoot(host)

    act(() => {
      root.render(createElement(Avatar, {
        userId: 7,
        displayName: 'Vanessa',
        avatarUrl: 'https://invalid.example/avatar.jpg',
      }))
    })
    act(() => {
      host.querySelector('img')?.dispatchEvent(new Event('error'))
    })

    expect(host.querySelector('.avatar-wave')).not.toBeNull()
    expect(host.querySelector('img')).toBeNull()
    act(() => {
      root.render(createElement(Avatar, {
        userId: 7,
        displayName: 'Vanessa',
        avatarUrl: 'https://valid.example/new-avatar.jpg',
      }))
    })
    expect(host.querySelector('img')?.getAttribute('src')).toBe('https://valid.example/new-avatar.jpg')
    act(() => root.unmount())
  })

  it('usa o avatar Sintonia quando não existe foto', () => {
    const host = document.createElement('div')
    document.body.appendChild(host)
    const root = createRoot(host)

    act(() => {
      root.render(createElement(Avatar, {
        userId: 3,
        displayName: 'Marina',
        avatarUrl: null,
      }))
    })

    expect(host.querySelector('img')).toBeNull()
    expect(host.querySelector('.avatar-wave')).not.toBeNull()
    expect(host.textContent).not.toContain('M')
    act(() => root.unmount())
  })
})
