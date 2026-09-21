import type { PlaybackMode } from '../types'

export function shouldPlay(
  mode: PlaybackMode,
  playerClientSessionId: string | null,
  mySessionId: string,
): boolean {
  if (mode === 'TODOS_OS_NAVEGADORES') {
    return true
  }
  return playerClientSessionId === mySessionId
}

export function shouldPauseLocally(mode: PlaybackMode): boolean {
  return mode === 'TODOS_OS_NAVEGADORES'
}

export function parseDurationSeconds(iso: string): number | null {
  const match = iso.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?/)
  if (!match) {
    return null
  }
  const hours = match[1] ? parseInt(match[1], 10) : 0
  const minutes = match[2] ? parseInt(match[2], 10) : 0
  const seconds = match[3] ? parseFloat(match[3]) : 0
  return Math.floor(hours * 3600 + minutes * 60 + seconds)
}

export function clampSeekSeconds(positionSeconds: number, durationSeconds: number | null): number {
  const nonNegative = Math.max(0, positionSeconds)
  if (durationSeconds == null || durationSeconds <= 0) {
    return nonNegative
  }
  return Math.min(nonNegative, Math.max(0, durationSeconds - 1))
}

export function resumeTargetSeconds(positionSeconds: number, durationIso: string): number {
  return clampSeekSeconds(positionSeconds, parseDurationSeconds(durationIso))
}

export function formatDuration(iso: string): string {
  const match = iso.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?/)
  if (!match) {
    return iso
  }
  const hours = match[1] ? parseInt(match[1], 10) : 0
  const minutes = match[2] ? parseInt(match[2], 10) : 0
  const seconds = match[3] ? Math.floor(parseFloat(match[3])) : 0
  if (hours > 0) {
    return `${hours}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
  }
  return `${minutes}:${String(seconds).padStart(2, '0')}`
}
