CREATE TABLE CALENDARS
(
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL UNIQUE,
    version    INT         NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    FOREIGN KEY (user_id) REFERENCES USERS (id)
);

ALTER TABLE TIME_SLOTS ADD COLUMN calendar_id UUID NOT NULL REFERENCES CALENDARS (id);

CREATE INDEX idx_slot_calendar_time ON TIME_SLOTS (calendar_id, start_time, end_time);
