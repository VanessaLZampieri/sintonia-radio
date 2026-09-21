CREATE UNIQUE INDEX IF NOT EXISTS ux_queue_items_playing_per_room
    ON queue_items (room_id)
    WHERE status = 'PLAYING';

ALTER TABLE queue_items ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE playbacks ADD COLUMN IF NOT EXISTS total_paused_millis BIGINT NOT NULL DEFAULT 0;
ALTER TABLE playbacks ADD COLUMN IF NOT EXISTS paused_at TIMESTAMPTZ;
