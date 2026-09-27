import { useId } from 'react'

export const LOGO_WAVE_PATH = 'M2 19c7 0 7-10 14-10s7 19 14 19S37 4 45 4s7 28 15 28S67 9 75 9s7 10 17 10'

export function LogoMark({ className }: { className: string }) {
  const gradientId = useId().replace(/:/g, '')

  return (
    <svg className={className} viewBox="0 0 94 36" aria-hidden="true" focusable="false">
      <defs>
        <linearGradient id={gradientId} x1="2" y1="18" x2="92" y2="18" gradientUnits="userSpaceOnUse">
          <stop stopColor="var(--identity-blue)" />
          <stop offset="0.38" stopColor="var(--identity-violet)" />
          <stop offset="0.68" stopColor="var(--pink)" />
          <stop offset="1" stopColor="var(--pink)" />
        </linearGradient>
      </defs>
      <path d={LOGO_WAVE_PATH} stroke={`url(#${gradientId})`} />
    </svg>
  )
}
