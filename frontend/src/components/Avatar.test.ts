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

  it('permite que usuários diferentes recebam variantes diferentes', () => {
    expect(avatarVariant(1)).not.toBe(avatarVariant(2))
    expect(new Set(Array.from({ length: 8 }, (_, id) => avatarVariant(id))).size).toBe(8)
  })

  it('ignora a foto Google e sempre renderiza o SVG Sintonia', () => {
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

    expect(host.querySelector('.avatar-wave')).not.toBeNull()
    expect(host.querySelector('img')).toBeNull()
    expect(host.querySelector('.avatar--variant-7')).not.toBeNull()
    act(() => root.unmount())
  })

  it('não renderiza foto nem inicial quando não existe avatar externo', () => {
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
