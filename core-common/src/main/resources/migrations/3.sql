CREATE TABLE IF NOT EXISTS lists (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(256),
    PRIMARY KEY(id),
    UNIQUE KEY(name)
);

CREATE TABLE IF NOT EXISTS list_player_relations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    player_uuid VARCHAR(36),
    list_id INT,
    relation ENUM("PENDING", "DECLINED", "ACCEPTED") NOT NULL,
    UNIQUE KEY(id),
    PRIMARY KEY(player_uuid, list_id),
    FOREIGN KEY(player_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
    FOREIGN KEY(list_id) REFERENCES lists(id) ON DELETE CASCADE
);