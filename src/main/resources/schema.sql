CREATE UNIQUE INDEX IF NOT EXISTS ux_queue_items_playing_per_room
    ON queue_items (room_id)
    WHERE status = 'PLAYING';
