CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    display_name    VARCHAR(100) NOT NULL,
    avatar_url      VARCHAR(500),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE events (
    id               BIGSERIAL PRIMARY KEY,
    owner_id         BIGINT       NOT NULL REFERENCES users (id),
    title            VARCHAR(200) NOT NULL,
    description      TEXT,
    type             VARCHAR(50)  NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PLANNING',
    start_at         TIMESTAMP,
    end_at           TIMESTAMP,
    location_name    VARCHAR(255),
    max_participants INTEGER,
    budget_amount    DECIMAL(10, 2),
    cover_image_url  VARCHAR(500),
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_events_owner_id ON events (owner_id);
CREATE INDEX idx_events_status ON events (status);
CREATE INDEX idx_events_type ON events (type);
CREATE INDEX idx_events_created_at ON events (created_at DESC);

CREATE TABLE event_participants (
    id          BIGSERIAL PRIMARY KEY,
    event_id    BIGINT    NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    user_id     BIGINT    NOT NULL REFERENCES users (id),
    role        VARCHAR(20) NOT NULL DEFAULT 'PARTICIPANT',
    rsvp_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    joined_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (event_id, user_id)
);

CREATE INDEX idx_event_participants_event_id ON event_participants (event_id);
CREATE INDEX idx_event_participants_user_id ON event_participants (user_id);
