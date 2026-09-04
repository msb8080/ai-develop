ALTER TABLE conversations ADD COLUMN updated_at TIMESTAMPTZ;
UPDATE conversations SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE conversations ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE messages ADD COLUMN request_id UUID;
ALTER TABLE messages ADD COLUMN metadata_json TEXT;

CREATE INDEX idx_conversations_updated ON conversations(updated_at DESC);
CREATE INDEX idx_messages_request ON messages(request_id);
