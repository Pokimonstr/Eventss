ALTER TABLE events ADD COLUMN IF NOT EXISTS latitude  DOUBLE PRECISION;
ALTER TABLE events ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

-- Умный выбор даты
CREATE TABLE event_date_options (
    id         BIGSERIAL PRIMARY KEY,
    event_id   BIGINT    NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    start_at   TIMESTAMP NOT NULL,
    end_at     TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_date_options_event_id ON event_date_options (event_id);

CREATE TABLE event_date_votes (
    id         BIGSERIAL PRIMARY KEY,
    option_id  BIGINT      NOT NULL REFERENCES event_date_options (id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users (id),
    choice     VARCHAR(10) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (option_id, user_id)
);

-- Разделение счетов
CREATE TABLE event_expenses (
    id         BIGSERIAL PRIMARY KEY,
    event_id   BIGINT         NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    payer_id   BIGINT         NOT NULL REFERENCES users (id),
    title      VARCHAR(200)   NOT NULL,
    amount     DECIMAL(12, 2) NOT NULL,
    created_at TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_expenses_event_id ON event_expenses (event_id);

CREATE TABLE event_expense_shares (
    id         BIGSERIAL PRIMARY KEY,
    expense_id BIGINT         NOT NULL REFERENCES event_expenses (id) ON DELETE CASCADE,
    user_id    BIGINT         NOT NULL REFERENCES users (id),
    amount     DECIMAL(12, 2) NOT NULL,
    UNIQUE (expense_id, user_id)
);

-- Чат мероприятия
CREATE TABLE event_chat_messages (
    id         BIGSERIAL PRIMARY KEY,
    event_id   BIGINT        NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    author_id  BIGINT        NOT NULL REFERENCES users (id),
    text       VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_messages_event_id ON event_chat_messages (event_id, id);

-- Геолокация участников
CREATE TABLE event_participant_locations (
    id         BIGSERIAL PRIMARY KEY,
    event_id   BIGINT           NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    user_id    BIGINT           NOT NULL REFERENCES users (id),
    latitude   DOUBLE PRECISION NOT NULL,
    longitude  DOUBLE PRECISION NOT NULL,
    status     VARCHAR(20)      NOT NULL,
    updated_at TIMESTAMP        NOT NULL DEFAULT NOW(),
    UNIQUE (event_id, user_id)
);

-- Чек-лист AI-ассистента
CREATE TABLE event_checklist_items (
    id         BIGSERIAL PRIMARY KEY,
    event_id   BIGINT       NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    text       VARCHAR(300) NOT NULL,
    category   VARCHAR(50)  NOT NULL,
    done       BOOLEAN      NOT NULL DEFAULT FALSE,
    generated  BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order INTEGER      NOT NULL DEFAULT 0,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_checklist_event_id ON event_checklist_items (event_id);

-- Альбом воспоминаний
CREATE TABLE event_photos (
    id            BIGSERIAL PRIMARY KEY,
    event_id      BIGINT       NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    uploader_id   BIGINT       NOT NULL REFERENCES users (id),
    stored_name   VARCHAR(200) NOT NULL,
    original_name VARCHAR(300) NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    caption       VARCHAR(300),
    taken_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_photos_event_id ON event_photos (event_id, taken_at DESC);
