export type RoomStatus = 'ACTIVE' | 'CLOSED'
export type PlaybackMode = 'TODOS_OS_NAVEGADORES' | 'CAIXA_DE_MUSICA'
export type QueueItemStatus = 'WAITING' | 'PLAYING' | 'FINISHED' | 'SKIPPED' | 'ERROR'
export type QueueItemSource = 'USER' | 'AUTO_DJ'
export type PlaybackStatus = 'PLAYING' | 'FINISHED' | 'SKIPPED' | 'ERROR'

export type RoomEventType =
  | 'PLAYBACK_STARTED'
  | 'PLAYBACK_FINISHED'
  | 'PLAYBACK_SKIPPED'
  | 'PLAYBACK_ERROR'
  | 'PLAYBACK_PAUSED'
  | 'PLAYBACK_RESUMED'
  | 'QUEUE_CHANGED'
  | 'MEMBERS_CHANGED'
  | 'PLAYER_CHANGED'
  | 'PLAYBACK_MODE_CHANGED'
  | 'SKIP_VOTE_CHANGED'

export interface Me {
  id: number
  name: string
  displayName: string
  email: string
  avatarUrl: string | null
}

export interface Room {
  id: number
  code: string
  name: string
  status: RoomStatus
  createdAt: string
}

export interface RoomMember {
  id: number
  roomId: number
  roomCode: string
  userId: number
  joinedAt: string
  leftAt: string | null
}

export interface RoomParticipant {
  userId: number
  displayName: string
  avatarUrl: string | null
  waitingCount: number
}

export type RoomActivityType =
  | 'MEMBER_JOINED'
  | 'MEMBER_LEFT'
  | 'SONG_ADDED'
  | 'SONG_REMOVED'
  | 'ROOM_RENAMED'
  | 'PLAYBACK_STARTED'
  | 'PLAYBACK_FINISHED'
  | 'PLAYBACK_SKIPPED'

export interface RoomActivity {
  id: number
  type: RoomActivityType
  actorUserId: number | null
  actorDisplayName: string | null
  songId: number | null
  songTitle: string | null
  detail: string | null
  createdAt: string
}

export interface ActiveRoom {
  roomId: number
  name: string
  code: string
  participantCount: number
  waitingCount: number
  nowPlaying: {
    title: string
    youtubeVideoId: string
    thumbnailUrl: string
    addedBy: { userId: number; displayName: string; avatarUrl: string | null } | null
  } | null
}

export interface UserRoom {
  roomId: number
  name: string
  code: string
  status: RoomStatus
  canEnter: boolean
  participantCount: number
}

export interface RoomSummary {
  roomId: number
  playbackCount: number
  participantCount: number
  skippedCount: number
  autoDjPlaybackCount: number
  skipVoteCount: number
  contributions: RoomContribution[]
}

export interface RoomContribution {
  userId: number
  displayName: string
  avatarUrl: string | null
  addedCount: number
  playedCount: number
  skippedCount: number
  skipVoteCount: number
}

export interface Song {
  id: number
  youtubeVideoId: string
  title: string
  thumbnailUrl: string
  duration: string
}

export interface SongSearchItem {
  videoId: string | null
  title: string | null
  channelTitle?: string | null
  thumbnailUrl?: string | null
  duration?: string | null
}

export interface AddedBy {
  id: number
  name: string
  avatarUrl: string | null
}

export interface QueueItem {
  id: number
  position: number
  addedAt: string
  song: Song
  addedBy: AddedBy | null
}

export interface RoomPlayer {
  roomId: number
  clientSessionId: string | null
  userId: number | null
  assumedAt: string | null
}

export interface PlaybackModeState {
  roomId: number
  mode: PlaybackMode
}

export interface SkipVote {
  playbackId: number
  votes: number
  requiredVotes: number
  skipped: boolean
}

export interface HistoryItem {
  playbackId: number
  queueItemId: number
  song: Song
  startedAt: string
  endedAt: string | null
  status: PlaybackStatus
  addedBy: AddedBy | null
}

export interface SongState {
  youtubeVideoId: string
  title: string
  thumbnailUrl: string
  duration: string
}

export interface RoomState {
  roomId: number
  roomCode: string
  name: string
  status: RoomStatus
  members: RoomParticipant[]
  playbackMode: PlaybackMode
  player: { clientSessionId: string | null; userId: number | null; assumedAt: string | null } | null
  currentPlayback: {
    playbackId: number
    queueItemId: number
    startedAt: string
    paused: boolean
    positionSeconds: number
    song: SongState
    addedByUserId: number | null
    addedByDisplayName: string | null
    source: QueueItemSource
  } | null
  queue: {
    queueItemId: number
    position: number
    status: QueueItemStatus
    song: SongState
    addedByUserId: number | null
    source: QueueItemSource
  }[]
  skipVote: { votes: number; requiredVotes: number; currentUserVoted: boolean } | null
}

export interface RoomEvent {
  eventType: RoomEventType
  roomId: number
  payload: unknown
}
