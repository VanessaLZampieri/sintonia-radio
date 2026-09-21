import type {
  HistoryItem,
  Me,
  PlaybackMode,
  PlaybackModeState,
  QueueItem,
  Room,
  RoomMember,
  RoomPlayer,
  RoomState,
  SkipVote,
  Song,
  SongSearchItem,
} from './types'
import { csrfHeaders } from './lib/csrf'

export async function logout(): Promise<void> {
  await fetch('/logout', {
    method: 'POST',
    credentials: 'same-origin',
    headers: csrfHeaders('POST', document.cookie),
  })
}

export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const method = (options.method ?? 'GET').toUpperCase()
  const headers: Record<string, string> = { ...csrfHeaders(method, document.cookie) }

  if (options.body != null) {
    headers['Content-Type'] = 'application/json'
  }
  headers['Accept'] = 'application/json'

  const response = await fetch(url, {
    ...options,
    method,
    headers,
    credentials: 'same-origin',
  })

  if (response.status === 204) {
    return undefined as T
  }

  const text = await response.text()
  const data = text ? JSON.parse(text) : null

  if (!response.ok) {
    const message =
      data && typeof data.message === 'string' ? data.message : `Erro inesperado (${response.status})`
    throw new ApiError(response.status, message)
  }

  return data as T
}

function jsonBody(value: unknown): string {
  return JSON.stringify(value)
}

export const api = {
  me: () => request<Me>('/api/me'),

  createRoom: () => request<Room>('/rooms', { method: 'POST' }),

  enterRoom: (code: string) =>
    request<RoomMember>(`/rooms/${encodeURIComponent(code)}/members`, { method: 'POST' }),

  leaveRoom: (code: string) =>
    request<void>(`/rooms/${encodeURIComponent(code)}/members`, { method: 'DELETE' }),

  roomState: (roomId: number) => request<RoomState>(`/api/rooms/${roomId}/state`),

  searchSongs: (query: string, maxResults = 10) =>
    request<SongSearchItem[]>(
      `/api/songs/search?q=${encodeURIComponent(query)}&maxResults=${maxResults}`,
    ),

  selectSong: (videoId: string) => request<Song>(`/api/songs/${encodeURIComponent(videoId)}`),

  addToQueue: (roomId: number, songId: number) =>
    request<QueueItem>(`/api/rooms/${roomId}/queue`, {
      method: 'POST',
      body: jsonBody({ songId }),
    }),

  removeFromQueue: (roomId: number, queueItemId: number) =>
    request<void>(`/api/rooms/${roomId}/queue/${queueItemId}`, { method: 'DELETE' }),

  currentPlayer: (roomId: number) => request<RoomPlayer>(`/api/rooms/${roomId}/player`),

  claimPlayer: (roomId: number, clientSessionId: string) =>
    request<RoomPlayer>(`/api/rooms/${roomId}/player`, {
      method: 'POST',
      body: jsonBody({ clientSessionId }),
    }),

  releasePlayer: (roomId: number, clientSessionId: string) =>
    request<RoomPlayer>(`/api/rooms/${roomId}/player`, {
      method: 'DELETE',
      body: jsonBody({ clientSessionId }),
    }),

  playbackMode: (roomId: number) => request<PlaybackModeState>(`/api/rooms/${roomId}/playback-mode`),

  changePlaybackMode: (roomId: number, mode: PlaybackMode) =>
    request<PlaybackModeState>(`/api/rooms/${roomId}/playback-mode`, {
      method: 'PUT',
      body: jsonBody({ mode }),
    }),

  skipVote: (roomId: number) =>
    request<SkipVote>(`/api/rooms/${roomId}/skip-votes`, { method: 'POST' }),

  currentSkipVote: (roomId: number) => request<SkipVote>(`/api/rooms/${roomId}/skip-votes`),

  history: (roomId: number, limit = 20) =>
    request<HistoryItem[]>(`/api/rooms/${roomId}/history?limit=${limit}`),

  finishPlayback: (playbackId: number, clientSessionId?: string) =>
    request<void>(`/api/playbacks/${playbackId}/finish`, {
      method: 'POST',
      body: jsonBody({ clientSessionId }),
    }),

  errorPlayback: (playbackId: number, clientSessionId?: string) =>
    request<void>(`/api/playbacks/${playbackId}/error`, {
      method: 'POST',
      body: jsonBody({ clientSessionId }),
    }),

  pausePlayback: (playbackId: number, clientSessionId?: string) =>
    request<void>(`/api/playbacks/${playbackId}/pause`, {
      method: 'POST',
      body: jsonBody({ clientSessionId }),
    }),

  resumePlayback: (playbackId: number, clientSessionId?: string) =>
    request<void>(`/api/playbacks/${playbackId}/resume`, {
      method: 'POST',
      body: jsonBody({ clientSessionId }),
    }),
}
