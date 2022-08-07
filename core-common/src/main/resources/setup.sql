CREATE TABLE IF NOT EXISTS players (
    uuid BINARY(16) NOT NULL,
    name VARCHAR(16) NOT NULL,
    reputation INT NOT NULL,
    global_color INT NOT NULL,
    PRIMARY KEY(uuid),
    UNIQUE KEY(name)
);

CREATE TABLE IF NOT EXISTS teams (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(256) NOT NULL,
    description TEXT,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    private BOOLEAN NOT NULL DEFAULT FALSE,
    owner_uuid BINARY(16) NOT NULL,
    PRIMARY KEY(id),
    UNIQUE KEY(name),
    KEY(verified),
    FOREIGN KEY(owner_uuid) REFERENCES players(uuid) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS team_members (
    member_uuid BINARY(16) NOT NULL,
    team_id INTEGER NOT NULL,
    PRIMARY KEY(member_uuid, team_id),
    FOREIGN KEY(member_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
    FOREIGN KEY(team_id) REFERENCES teams(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS team_invitations (
    invited_uuid BINARY(16) NOT NULL,
    team_id INTEGER NOT NULL,
    PRIMARY KEY(invited_uuid, team_id),
    FOREIGN KEY(invited_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
    FOREIGN KEY(team_id) REFERENCES teams(id) ON DELETE CASCADE
);