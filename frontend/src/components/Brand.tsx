import { LogoMark } from './LogoMark'

export function Brand() {
  return (
    <div className="brand">
      <LogoMark className="brand-wave" />
      <span className="brand-text">
        <span className="brand-name">sintonia</span>
        <span className="brand-tag">rádio compartilhada</span>
      </span>
    </div>
  )
}
