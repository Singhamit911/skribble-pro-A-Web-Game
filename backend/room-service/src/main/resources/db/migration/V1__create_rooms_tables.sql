CREATE TABLE rooms (
    id              UUID PRIMARY KEY,
    code            VARCHAR(6) UNIQUE NOT NULL,
    host_guest_id   VARCHAR(36) NOT NULL,
    host_name       VARCHAR(50) NOT NULL,
    status          VARCHAR(20) DEFAULT 'WAITING' NOT NULL,
    max_players     INT DEFAULT 8 NOT NULL,
    rounds          INT DEFAULT 3 NOT NULL,
    draw_time       INT DEFAULT 80 NOT NULL,
    category        VARCHAR(50) DEFAULT 'General' NOT NULL,
    created_at      TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_rooms_code ON rooms(code);

CREATE TABLE room_players (
    room_id     UUID NOT NULL,
    guest_id    VARCHAR(36) NOT NULL,
    guest_name  VARCHAR(50) NOT NULL,
    color       VARCHAR(7) NOT NULL,
    joined_at   TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (room_id, guest_id),
    CONSTRAINT fk_room_players_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE
);

CREATE INDEX idx_room_players_room_id ON room_players(room_id);
