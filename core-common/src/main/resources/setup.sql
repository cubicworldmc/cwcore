CREATE TABLE IF NOT EXISTS players (
    uuid VARCHAR(36),
    name VARCHAR(16) NOT NULL,
    reputation INT NOT NULL DEFAULT 0,
    global_color INT NOT NULL DEFAULT -1,
    selected_team_id INT DEFAULT NULL,
    PRIMARY KEY(uuid),
    UNIQUE KEY(name)
);

CREATE TABLE IF NOT EXISTS teams (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(256) NOT NULL,
    description TEXT DEFAULT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    hide BOOLEAN NOT NULL DEFAULT FALSE,
    owner_uuid VARCHAR(36) NOT NULL,
    PRIMARY KEY(id),
    UNIQUE KEY(name),
    KEY(verified),
    FOREIGN KEY(owner_uuid) REFERENCES players(uuid) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS team_player_relations (
    player_uuid VARCHAR(36),
    team_id INTEGER,
    relation ENUM("INVITE", "MEMBERSHIP") NOT NULL,
    PRIMARY KEY(player_uuid, team_id),
    FOREIGN KEY(player_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
    FOREIGN KEY(team_id) REFERENCES teams(id) ON DELETE CASCADE
);
