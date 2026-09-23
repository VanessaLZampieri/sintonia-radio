CREATE UNIQUE INDEX IF NOT EXISTS ux_queue_items_playing_per_room
    ON queue_items (room_id)
    WHERE status = 'PLAYING';

ALTER TABLE queue_items ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE playbacks ADD COLUMN IF NOT EXISTS total_paused_millis BIGINT NOT NULL DEFAULT 0;
ALTER TABLE playbacks ADD COLUMN IF NOT EXISTS paused_at TIMESTAMPTZ;

ALTER TABLE users ADD COLUMN IF NOT EXISTS display_name VARCHAR(255);
UPDATE users
    SET display_name = btrim(split_part(btrim(COALESCE(name, '')), ' ', 1))
    WHERE display_name IS NULL OR btrim(display_name) = '';
ALTER TABLE users ALTER COLUMN display_name SET NOT NULL;

ALTER TABLE rooms ADD COLUMN IF NOT EXISTS name VARCHAR(255);
UPDATE rooms
    SET name = 'Sala ' || code
    WHERE name IS NULL OR btrim(name) = '';
ALTER TABLE rooms ALTER COLUMN name SET NOT NULL;
