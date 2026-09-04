CREATE TABLE agent_runs (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL UNIQUE,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL,
    current_phase VARCHAR(32) NOT NULL,
    iteration INTEGER NOT NULL CHECK (iteration BETWEEN 1 AND 3),
    context_sources_json TEXT NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ
);

CREATE TABLE agent_events (
    id BIGSERIAL PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES agent_runs(id) ON DELETE CASCADE,
    sequence_number INTEGER NOT NULL,
    phase VARCHAR(32) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    payload_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (run_id, sequence_number)
);

CREATE INDEX idx_agent_runs_conversation ON agent_runs(conversation_id, started_at DESC);
CREATE INDEX idx_agent_events_run ON agent_events(run_id, sequence_number);
