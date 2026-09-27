import type { ReactNode } from 'react'

export function avatarVariant(userId: number): number {
  return Math.abs(userId) % 8
}

function motif(variant: number): ReactNode {
  switch (variant) {
    case 0:
      return (
        <>
          <rect x="9" y="8" width="3" height="16" rx="1.5" fill="currentColor" />
          <rect x="14.5" y="5" width="3" height="19" rx="1.5" fill="currentColor" />
          <rect x="20" y="11" width="3" height="13" rx="1.5" fill="currentColor" />
        </>
      )
    case 1:
      return (
        <>
          <circle cx="16" cy="16" r="10" fill="none" stroke="currentColor" strokeWidth="2" />
          <circle cx="16" cy="16" r="5.5" fill="none" stroke="currentColor" strokeWidth="2" />
          <circle cx="16" cy="16" r="1.8" fill="currentColor" />
        </>
      )
    case 2:
      return (
        <path
          d="M5 16h2l2-6 3 12 3-9 3 9 2-6h5"
          fill="none"
          stroke="currentColor"
          strokeWidth="2.4"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      )
    case 3:
      return (
        <path
          d="M16 5l6.5 11h-13z"
          fill="none"
          stroke="currentColor"
          strokeWidth="2.4"
          strokeLinejoin="round"
        />
      )
    case 4:
      return (
        <>
          <circle cx="16" cy="16" r="10" fill="none" stroke="currentColor" strokeWidth="2" />
          <path d="M11.5 20.5L20.5 11.5" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" />
          <circle cx="11.5" cy="20.5" r="1.8" fill="currentColor" />
          <circle cx="20.5" cy="11.5" r="1.8" fill="currentColor" />
        </>
      )
    case 5:
      return (
        <>
          <rect x="8" y="8" width="6" height="16" rx="2" fill="currentColor" />
          <rect x="18" y="8" width="6" height="16" rx="2" fill="currentColor" />
        </>
      )
    case 6:
      return (
        <path
          d="M6 16a10 10 0 0 1 20 0"
          fill="none"
          stroke="currentColor"
          strokeWidth="2.4"
          strokeLinecap="round"
        />
      )
    default:
      return (
        <>
          <path d="M8 20l4-9 4 9 4-13 4 13" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
        </>
      )
  }
}

export function Avatar({
  userId,
  displayName,
  size,
}: {
  userId: number
  displayName: string
  avatarUrl: string | null
  size?: 'sm'
}) {
  const variant = avatarVariant(userId)
  const className = `avatar avatar--variant-${variant}${size === 'sm' ? ' avatar--sm' : ''}`

  return (
    <span className={className} aria-hidden="true" title={displayName}>
      <svg className="avatar-wave" viewBox="0 0 32 32" focusable="false">
        {motif(variant)}
      </svg>
    </span>
  )
}
