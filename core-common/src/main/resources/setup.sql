CREATE TABLE IF NOT EXISTS players (
    uuid_most BIGINT NOT NULL,
    uuid_least BIGINT NOT NULL,
    name VARCHAR(16) NOT NULL,
    global_color INTEGER DEFAULT NULL,
    reputation INTEGER NOT NULL DEFAULT 0,
    UNIQUE KEY(uuid_most, uuid_least),
    UNIQUE KEY(name)
);