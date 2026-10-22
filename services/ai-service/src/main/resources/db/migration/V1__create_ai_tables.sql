CREATE TABLE IF NOT EXISTS ai_audit_events (
    id UUID PRIMARY KEY,
    trace_id VARCHAR(255) NOT NULL,
    feature_type VARCHAR(64) NOT NULL,
    user_id UUID NOT NULL,
    course_id UUID,
    lesson_id UUID,
    status VARCHAR(64) NOT NULL,
    reason VARCHAR(128),
    provider VARCHAR(64),
    model VARCHAR(128),
    input_tokens INTEGER,
    output_tokens INTEGER,
    total_tokens INTEGER,
    estimated_cost NUMERIC,
    latency_ms BIGINT,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS ai_conversations (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    user_id UUID NOT NULL,
    title VARCHAR(150) NOT NULL DEFAULT 'New conversation'
);

CREATE TABLE IF NOT EXISTS ai_messages (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    conversation_id UUID NOT NULL REFERENCES ai_conversations(id),
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    client_message_id VARCHAR(100),
    CONSTRAINT uk_ai_messages_conversation_client_message_id UNIQUE (conversation_id, client_message_id)
);

CREATE INDEX IF NOT EXISTS idx_ai_conversations_user_created_at ON ai_conversations(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_ai_messages_conversation_created_at ON ai_messages(conversation_id, created_at);
CREATE INDEX IF NOT EXISTS idx_ai_messages_conversation_client_message_id ON ai_messages(conversation_id, client_message_id);
