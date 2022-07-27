CREATE TABLE IF NOT EXISTS players (
    uuid_most BIGINT NOT NULL,
    uuid_least BIGINT NOT NULL,
    name VARCHAR(16) NOT NULL,
    global_color INTEGER DEFAULT NULL,
    reputation INTEGER NOT NULL DEFAULT 0,
    UNIQUE KEY(uuid_most, uuid_least),
    UNIQUE KEY(name)
);

CREATE TABLE IF NOT EXISTS teams (
    id INTEGER NOT NULL AUTO_INCREMENT,
    name VARCHAR(16) NOT NULL,
    description TEXT,
    owner_uuid_most BIGINT NOT NULL,
    owner_uuid_least BIGINT NOT NULL,
    PRIMARY KEY(id),
    UNIQUE KEY(name)
);

CREATE TABLE IF NOT EXISTS team_applications (
    name VARCHAR(16) NOT NULL,
    uuid_most BIGINT NOT NULL,
    uuid_least BIGINT NOT NULL,
    UNIQUE KEY (name),
    KEY(uuid_most, uuid_least)
);

CREATE TABLE IF NOT EXISTS team_members (
    id INTEGER NOT NULL,
    uuid_most BIGINT NOT NULL,
    uuid_least BIGINT NOT NULL,
    KEY(id),
    KEY(uuid_most, uuid_least)
);