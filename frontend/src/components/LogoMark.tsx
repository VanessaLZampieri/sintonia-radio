export const LOGO_WAVE_PATH = 'M2 19c7 0 7-10 14-10s7 19 14 19S37 4 45 4s7 28 15 28S67 9 75 9s7 10 17 10'

export function LogoMark({ className }: { className: string }) {
  return (
    <svg className={className} viewBox="0 0 94 36" aria-hidden="true" focusable="false">
      <path d={LOGO_WAVE_PATH} />
    </svg>
  )
}
