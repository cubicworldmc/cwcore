CREATE TABLE IF NOT EXISTS players (
    uuid VARCHAR(36),
    name VARCHAR(16) NOT NULL,
    reputation INT NOT NULL,
    global_color INT NOT NULL,
    selected_team_id INT,
    PRIMARY KEY(uuid),
    UNIQUE KEY(name)
);

CREATE TABLE IF NOT EXISTS teams (
    id INT NOT NULL, -- actually AUTO_INCREMENT
    name VARCHAR(256) NOT NULL,
    description TEXT,
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
    team_id INTEGER NOT NULL,
    relation ENUM("INVITE", "MEMBERSHIP") NOT NULL,
    PRIMARY KEY(player_uuid, team_id),
    FOREIGN KEY(player_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
    FOREIGN KEY(team_id) REFERENCES teams(id) ON DELETE CASCADE
);
