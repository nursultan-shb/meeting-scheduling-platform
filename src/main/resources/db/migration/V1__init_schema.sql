CREATE TABLE USERS
(
    id         UUID PRIMARY KEY,
    email      VARCHAR(255) UNIQUE NOT NULL,
    name       VARCHAR(255)        NOT NULL,
    created_at TIMESTAMPTZ         NOT NULL,
    version    INT                 NOT NULL
);

CREATE TABLE TIME_SLOTS
(
    id         UUID PRIMARY KEY,
    start_time TIMESTAMPTZ NOT NULL,
    end_time   TIMESTAMPTZ NOT NULL,
    status     VARCHAR(20) NOT NULL,
    meeting_id UUID NULL,
    version    INT         NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE MEETINGS
(
    id           UUID PRIMARY KEY,
    time_slot_id UUID         NOT NULL,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    created_at   TIMESTAMPTZ  NOT NULL,
    version      INT          NOT NULL,

    FOREIGN KEY (time_slot_id) REFERENCES TIME_SLOTS (id)
);

CREATE TABLE MEETING_PARTICIPANTS
(
    id         UUID PRIMARY KEY,
    meeting_id UUID NOT NULL,
    user_id    UUID NOT NULL,

    CONSTRAINT unique_meeting_id_user_id UNIQUE (meeting_id, user_id),

    FOREIGN KEY (meeting_id) REFERENCES meetings (id),
    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_participant_user ON MEETING_PARTICIPANTS (user_id);

ALTER TABLE TIME_SLOTS
    ADD FOREIGN KEY (meeting_id) REFERENCES meetings (id);

ALTER TABLE MEETINGS
    ADD CONSTRAINT uq_meeting_time_slot UNIQUE (time_slot_id);