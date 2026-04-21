CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    token_id VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES app_users(id),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE checkpoints (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    governorate VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    current_status VARCHAR(32) NOT NULL,
    notes VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE checkpoint_status_history (
    id UUID PRIMARY KEY,
    checkpoint_id UUID NOT NULL REFERENCES checkpoints(id),
    status VARCHAR(32) NOT NULL,
    notes VARCHAR(500),
    changed_by UUID REFERENCES app_users(id),
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE incidents (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1500) NOT NULL,
    category VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    verified BOOLEAN NOT NULL,
    reported_at TIMESTAMP WITH TIME ZONE NOT NULL,
    verified_at TIMESTAMP WITH TIME ZONE,
    closed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    checkpoint_id UUID REFERENCES checkpoints(id),
    created_by UUID REFERENCES app_users(id)
);

CREATE TABLE crowd_reports (
    id UUID PRIMARY KEY,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    category VARCHAR(64) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    status VARCHAR(32) NOT NULL,
    confidence_score DOUBLE PRECISION NOT NULL,
    abuse_flag_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    client_fingerprint VARCHAR(120),
    duplicate_of_report_id UUID,
    submitted_by UUID REFERENCES app_users(id),
    reviewed_by UUID REFERENCES app_users(id)
);

CREATE TABLE report_votes (
    id UUID PRIMARY KEY,
    report_id UUID NOT NULL REFERENCES crowd_reports(id),
    user_id UUID NOT NULL REFERENCES app_users(id),
    vote_type VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_report_vote UNIQUE (report_id, user_id)
);

CREATE TABLE moderation_audit (
    id UUID PRIMARY KEY,
    entity_type VARCHAR(32) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(255) NOT NULL,
    reason VARCHAR(500),
    moderator_id UUID REFERENCES app_users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE alert_subscriptions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id),
    area_name VARCHAR(255) NOT NULL,
    min_latitude DOUBLE PRECISION NOT NULL,
    max_latitude DOUBLE PRECISION NOT NULL,
    min_longitude DOUBLE PRECISION NOT NULL,
    max_longitude DOUBLE PRECISION NOT NULL,
    incident_category VARCHAR(64),
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE alert_records (
    id UUID PRIMARY KEY,
    subscription_id UUID NOT NULL REFERENCES alert_subscriptions(id),
    incident_id UUID NOT NULL REFERENCES incidents(id),
    delivery_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_checkpoint_governorate_status ON checkpoints(governorate, current_status);
CREATE INDEX idx_incident_status_verified ON incidents(status, verified);
CREATE INDEX idx_incident_location ON incidents(latitude, longitude);
CREATE INDEX idx_report_status_created ON crowd_reports(status, created_at);
CREATE INDEX idx_alert_subscription_active ON alert_subscriptions(active);
