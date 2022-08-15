package space.cubicworld.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.SneakyThrows;
import net.kyori.adventure.text.format.TextColor;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

public class CoreDatabaseImpl implements CoreDatabase {

    private final HikariDataSource dataSource;

    private final CoreCache<UUID, String, CorePlayer> playerCache = new CoreCache<>(
            new CoreCache.Functions<>() {
                @Override
                public String getSecondary(CorePlayer model) {
                    return model.getName();
                }

                @Override
                public UUID getKey(CorePlayer model) {
                    return model.getId();
                }

                @Override
                public CorePlayer fetch(ResultSet resultSet) throws SQLException {
                    CorePlayerImpl result = new CorePlayerImpl(
                            CoreDatabaseImpl.this,
                            UUID.fromString(resultSet.getString(1)),
                            resultSet.getString(2)
                    );
                    result.setReputation(resultSet.getInt(3));
                    Integer globalColor = resultSet.getObject(4, Integer.class);
                    result.setGlobalColor(globalColor == null ? null : TextColor.color(globalColor));
                    result.setSelectedTeamId(resultSet.getObject(5, Integer.class));
                    return result;
                }

                @Override
                public Connection getConnection() {
                    return CoreDatabaseImpl.this.getConnection();
                }

                @Override
                public String getKeyStatement() {
                    return "SELECT * FROM players WHERE uuid = ?";
                }

                @Override
                public String getSecondaryStatement() {
                    return "SELECT * FROM players WHERE name = ?";
                }

                @Override
                public void update(CorePlayer model) throws SQLException {
                    try (Connection connection = getConnection();
                         PreparedStatement statement = connection.prepareStatement("""
                                 UPDATE players
                                 SET name = ?, reputation = ?, global_color = ?, selected_team_id = ?
                                 WHERE uuid = ?
                                 """
                         )
                    ) {
                        statement.setString(1, model.getName());
                        statement.setInt(2, model.getReputation());
                        TextColor color = model.getGlobalColor();
                        statement.setObject(3, color == null ? null : color.value());
                        statement.setObject(4, model.getSelectedTeamId());
                        statement.setString(5, model.getId().toString());
                        statement.executeUpdate();
                    }
                }

                @Override
                public void remove(CorePlayer model) throws SQLException {
                    try (Connection connection = getConnection();
                         PreparedStatement statement = connection.prepareStatement("""
                                 DELETE FROM players WHERE uuid = ?
                                 """)
                    ) {
                        statement.setString(1, model.getId().toString());
                        statement.executeUpdate();
                    }
                }
            }
    );

    private final CoreCache<Integer, String, CoreTeam> teamCache = new CoreCache<>(
            new CoreCache.Functions<>() {
                @Override
                public String getSecondary(CoreTeam model) {
                    return model.getName();
                }

                @Override
                public Integer getKey(CoreTeam model) {
                    return model.getId();
                }

                @Override
                public CoreTeam fetch(ResultSet resultSet) throws SQLException {
                    CoreTeamImpl result = new CoreTeamImpl(
                            CoreDatabaseImpl.this,
                            resultSet.getInt(1),
                            resultSet.getString(2),
                            UUID.fromString(resultSet.getString(6))
                    );
                    result.setDescription(resultSet.getString(3));
                    result.setVerified(resultSet.getBoolean(4));
                    result.setHide(resultSet.getBoolean(5));
                    return result;
                }

                @Override
                public Connection getConnection() {
                    return CoreDatabaseImpl.this.getConnection();
                }

                @Override
                public String getKeyStatement() {
                    return "SELECT * FROM teams WHERE id = ?";
                }

                @Override
                public String getSecondaryStatement() {
                    return "SELECT * FROM teams WHERE name = ?";
                }

                @Override
                public void update(CoreTeam model) throws SQLException {
                    try (Connection connection = getConnection();
                         PreparedStatement statement = connection.prepareStatement("""
                                 UPDATE teams
                                 SET name = ?, description = ?, verified = ?, hide = ?, owner_uuid = ?
                                 WHERE id = ?
                                 """)) {
                        statement.setString(1, model.getName());
                        statement.setString(2, model.getDescription());
                        statement.setBoolean(3, model.isVerified());
                        statement.setBoolean(4, model.isHide());
                        statement.setString(5, model.getOwnerId().toString());
                        statement.setInt(6, model.getId());
                        statement.executeUpdate();
                    }
                }

                @Override
                public void remove(CoreTeam model) throws SQLException {
                    try (Connection connection = getConnection();
                         PreparedStatement statement = connection.prepareStatement("""
                                 DELETE FROM teams WHERE id = ?
                                 """)) {
                        statement.setInt(1, model.getId());
                        statement.executeUpdate();
                    }
                }
            }
    );
    @Getter
    private final CoreRelationCache relationCache = new CoreRelationCache(this);

    public CoreDatabaseImpl(
            String host,
            String username,
            String password,
            String database,
            ClassLoader classLoader,
            Logger logger
    ) throws IOException, SQLException {
        HikariConfig config = new HikariConfig();
        try (InputStream hikariPropertiesIs = classLoader.getResourceAsStream("hikari.properties")) {
            if (hikariPropertiesIs != null) {
                Properties hikariProperties = new Properties();
                hikariProperties.load(hikariPropertiesIs);
                config.setDataSourceProperties(hikariProperties);
            } else {
                logger.warn("Can not found hikari.properties resource");
            }
        } catch (IOException e) {
            logger.warn("Failed to load hikari.properties resource: ", e);
        }
        config.setJdbcUrl("jdbc:mysql://%s/%s".formatted(host, database));
        config.setUsername(username);
        config.setPassword(password);
        dataSource = new HikariDataSource(config);
        try (Connection connection = getConnection();
            Statement statement = connection.createStatement();
            InputStream setupSqlIs = classLoader.getResourceAsStream("setup.sql")) {
            String setupSqlString = new String(setupSqlIs.readAllBytes());
            String[] setupSqlStatements = setupSqlString.split(";");
            for (String setupSqlStatementSql: setupSqlStatements) {
                if (setupSqlStatementSql.trim().isEmpty()) break;
                statement.addBatch(setupSqlStatementSql);
            }
            statement.executeLargeBatch();
        }
    }

    @Override
    @SneakyThrows
    public Connection getConnection() {
        return dataSource.getConnection();
    }

    @Override
    @SneakyThrows
    public Optional<CorePlayer> fetchPlayer(UUID id) {
        return playerCache.fetchByKey(id);
    }

    @Override
    @SneakyThrows
    public Optional<CorePlayer> fetchPlayer(String name) {
        return playerCache.fetchBySecondary(name);
    }

    @Override
    @SneakyThrows
    public List<CorePlayer> fetchPlayers(String sql, Object... objects) {
        return playerCache.justFetch(sql, objects);
    }

    @Override
    @SneakyThrows
    public Optional<CoreTeam> fetchTeam(int id) {
        return teamCache.fetchByKey(id);
    }

    @Override
    @SneakyThrows
    public Optional<CoreTeam> fetchTeam(String name) {
        return teamCache.fetchBySecondary(name);
    }

    @Override
    @SneakyThrows
    public List<CoreTeam> fetchTeams(String sql, Object... objects) {
        return teamCache.justFetch(sql, objects);
    }

    @Override
    public Optional<CorePTRelation> fetchPTRelation(UUID player, int team) {
        return fetchTeam(team).isEmpty() || fetchPlayer(player).isEmpty() ?
                Optional.empty() : Optional.of(relationCache.fetchRelation(player, team));
    }

    @Override
    @SneakyThrows
    public CoreTeam newTeam(String name, UUID owner) {
        if (fetchTeam(name).isPresent()) {
            throw new IllegalArgumentException("Team with this name already exists");
        }
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO teams (name, owner_uuid) VALUES (?, ?)",
                     Statement.RETURN_GENERATED_KEYS
             )
        ) {
            statement.setString(1, name);
            statement.setString(2, owner.toString());
            statement.executeUpdate();
            ResultSet keys = statement.getGeneratedKeys();
            if (!keys.next()) {
                throw new IllegalStateException("Team with this name already exists");
            }
            int teamId = keys.getInt(1);
            keys.close();
            CoreTeam team = new CoreTeamImpl(this, teamId, name, owner);
            teamCache.cache(team);
            return team;
        }
    }

    @Override
    @SneakyThrows
    public CorePlayer newPlayer(UUID id, String name) {
        if (fetchPlayer(name).isPresent() || fetchPlayer(id).isPresent()) {
            throw new IllegalArgumentException("Player with this id or name already exists");
        }
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO players (uuid, name) VALUES (?, ?)"
             )
        ) {
            statement.setString(1, id.toString());
            statement.setString(2, name);
            statement.executeUpdate();
            CorePlayer player = new CorePlayerImpl(this, id, name);
            playerCache.cache(player);
            return player;
        }
    }

    @Override
    @SneakyThrows
    public void update(CorePlayer player) {
        playerCache.update(player);
    }

    @Override
    @SneakyThrows
    public void update(CoreTeam team) {
        teamCache.update(team);
    }

    @Override
    @SneakyThrows
    public void update(CorePTRelation relation) {
        CorePTRelation.Value value = relation.getValue();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(value == CorePTRelation.Value.NONE ?
                     "DELETE FROM team_player_relations WHERE player_uuid = ? AND team_id = ?" :
                     """
                             INSERT INTO team_player_relations (player_uuid, team_id, relation) VALUES (?, ?, ?)
                             ON DUPLICATE KEY UPDATE relation = ?
                             """
             )
        ) {
            statement.setString(1, relation.getPlayerId().toString());
            statement.setInt(2, relation.getTeamId());
            if (value != CorePTRelation.Value.NONE) {
                statement.setString(3, value.name());
                statement.setString(4, value.name());
            }
            statement.executeUpdate();
        }
    }

    @Override
    @SneakyThrows
    public void remove(CorePlayer player) {
        playerCache.remove(player);
    }

    @Override
    @SneakyThrows
    public void remove(CoreTeam team) {
        teamCache.remove(team);
    }
}
