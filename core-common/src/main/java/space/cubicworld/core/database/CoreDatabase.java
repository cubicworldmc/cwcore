package space.cubicworld.core.database;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import space.cubicworld.core.util.ImmutablePair;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class CoreDatabase implements AutoCloseable {

    @FunctionalInterface
    interface FetchFunction<T> {
        T fetch(CoreDatabase database, ResultSet resultSet, int start) throws SQLException;
    }

    private static CacheBuilder<Object, Object> defaultCacheBuilder(int maximumSize, Duration duration) {
        return CacheBuilder
                .newBuilder()
                .maximumSize(maximumSize == -1 ? 1000 : maximumSize)
                .expireAfterAccess(duration == null ? Duration.ofHours(1) : duration);
    }

    @Getter(AccessLevel.PRIVATE)
    private final HikariDataSource dataSource;

    private final LoadingCache<String, Optional<UUID>> playerNameToIdCache;
    private final LoadingCache<UUID, CorePlayer> playersCache;
    private final LoadingCache<String, Optional<Integer>> teamNameToIdCache;
    private final LoadingCache<Integer, CoreTeam> teamsCache;
    private final LoadingCache<ImmutablePair<UUID, Integer>, CorePlayerTeamRelation> relations;

    private final AtomicInteger teamIdCounter = new AtomicInteger();

    public CoreDatabase(
            String host,
            String username,
            String password,
            String database,
            ClassLoader classLoader,
            Logger logger
    ) throws SQLException, IOException {
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
        playersCache = defaultCacheBuilder(-1, null)
                .<UUID, CorePlayer>removalListener(notification -> notification.getValue().close())
                .build(
                        CacheLoader.from(uuid -> new CorePlayer(uuid, this))
                );
        playerNameToIdCache = defaultCacheBuilder(-1, null).build(
                CacheLoader.from(name -> {
                    CorePlayer player = fetchPlayerQuery("SELECT * FROM players WHERE name = ?", name);
                    if (player == null) return Optional.empty();
                    CorePlayer cached = playersCache.getIfPresent(player.getUuid());
                    if (cached != null) cached.setFetched(player.getFetched());
                    else playersCache.put(player.getUuid(), player);
                    return Optional.of(player.getUuid());
                })
        );
        teamsCache = defaultCacheBuilder(-1, null)
                .<Integer, CoreTeam>removalListener(notification -> notification.getValue().close())
                .build(
                        CacheLoader.from(id -> new CoreTeam(id, this))
                );
        teamNameToIdCache = defaultCacheBuilder(-1, null).build(
                CacheLoader.from(name -> {
                    CoreTeam team = fetchTeamQuery("SELECT * FROM teams WHERE name = ?", name);
                    if (team == null) return Optional.empty();
                    CoreTeam cached = teamsCache.getIfPresent(team.getId());
                    if (cached != null) cached.setFetched(team.getFetched());
                    else teamsCache.put(team.getId(), team);
                    return Optional.of(team.getId());
                })
        );
        relations = defaultCacheBuilder(10000, null).build(
                CacheLoader.from(key -> {
                    UUID player = key.getFirst();
                    int team = key.getSecond();
                    try (Connection connection = getConnection();
                         PreparedStatement statement = connection.prepareStatement(
                                 "SELECT relation FROM team_player_relations WHERE player_uuid = unhex(replace(?, '-', '')) AND team_id = ?"
                         )) {
                        statement.setString(1, player.toString());
                        statement.setInt(2, team);
                        ResultSet resultSet = statement.executeQuery();
                        CorePlayerTeamRelation.Relation relation = resultSet.next() ?
                                CorePlayerTeamRelation.Relation.valueOf(resultSet.getString(1)) :
                                CorePlayerTeamRelation.Relation.NONE;
                        CorePlayerTeamRelation result = new CorePlayerTeamRelation(player, team, this);
                        result.setRelation(relation);
                        return result;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                })
        );
        try (Connection connection = getConnection();
             Statement selectLastTeamId = connection.createStatement();
             Statement update = connection.createStatement()) {
            try (InputStream setupSqlIs = classLoader.getResourceAsStream("setup.sql")) {
                String setupSqlString = new String(setupSqlIs.readAllBytes());
                String[] setupSqlStatements = setupSqlString.split(";");
                for (String statement: setupSqlStatements) {
                    if (statement.trim().isEmpty()) break;
                    update.addBatch(statement + ";");
                }
            }
            update.executeLargeBatch();
            ResultSet selectLastTeamIdResultSet = selectLastTeamId
                    .executeQuery("SELECT id FROM teams ORDER BY id LIMIT 1");
            teamIdCounter.set(selectLastTeamIdResultSet.next() ?
                    selectLastTeamIdResultSet.getInt(1) + 1 : 0);
            selectLastTeamIdResultSet.close();
        }
    }

    @Override
    public void close() {
        dataSource.close();
        playersCache.asMap().forEach((uuid, player) -> player.close());
        teamsCache.asMap().forEach((id, team) -> team.close());
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @SneakyThrows
    private <T> T fetch(FetchFunction<T> fetchFunction, String sql, Object... objects) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int counter = 0;
            for (Object obj : objects) {
                statement.setObject(++counter, obj);
            }
            ResultSet resultSet = statement.executeQuery();
            T result = fetchFunction.fetch(this, resultSet, 1);
            resultSet.close();
            return result;
        }
    }

    public CorePlayer fetchPlayerQuery(String sql, Object... objects) {
        return fetch(CorePlayer::fromResultSet, sql, objects);
    }

    public CoreTeam fetchTeamQuery(String sql, Object... objects) {
        return fetch(CoreTeam::fromResultSet, sql, objects);
    }

    @SneakyThrows
    public CorePlayer fetchPlayerByUuid(UUID uuid) {
        return playersCache.get(uuid);
    }

    @SneakyThrows
    public CorePlayer fetchPlayerByName(String name) {
        Optional<UUID> id = playerNameToIdCache.get(name);
        if (id.isEmpty()) return null;
        return fetchPlayerByUuid(id.get());
    }

    public Optional<CorePlayer> fetchOptionalPlayerByName(String name) {
        try {
            return Optional.ofNullable(fetchPlayerByName(name));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void cache(CorePlayer player) {
        playersCache.put(player.getUuid(), player);
        playerNameToIdCache.put(player.getName(), Optional.of(player.getUuid()));
    }

    @SneakyThrows
    public CoreTeam fetchTeamById(int id) {
        return teamsCache.get(id);
    }

    @SneakyThrows
    public CoreTeam fetchTeamByName(String name) {
        Optional<Integer> id = teamNameToIdCache.get(name);
        if (id.isEmpty()) return null;
        return teamsCache.get(id.get());
    }

    public Optional<CoreTeam> fetchOptionalTeamByName(String name) {
        try {
            return Optional.ofNullable(fetchTeamByName(name));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void cache(CoreTeam team) {
        teamsCache.put(team.getId(), team);
        teamNameToIdCache.put(team.getName(), Optional.of(team.getId()));
    }

    public CoreTeam newTeam(String name) {
        if (fetchOptionalTeamByName(name).isPresent()) {
            throw new IllegalArgumentException("Team with this name already exists");
        }
        CoreTeam result = new CoreTeam(teamIdCounter.getAndIncrement(), this);
        result.setFetched(new CoreTeam.Fetched());
        result.getFetched().setName(name);
        cache(result);
        return result;
    }

    public CorePlayer newPlayer(UUID uuid, String name) {
        if (fetchOptionalPlayerByName(name).isPresent() || fetchPlayerByUuid(uuid).isActuallyExists()) {
            throw new IllegalArgumentException("Player with this name or uuid already exists");
        }
        CorePlayer result = new CorePlayer(uuid, this);
        result.setFetched(new CorePlayer.Fetched());
        result.getFetched().setName(name);
        cache(result);
        return result;
    }

    @SneakyThrows
    public CorePlayerTeamRelation fetchRelation(UUID uuid, int teamId) {
        return relations.get(new ImmutablePair<>(uuid, teamId));
    }

    public void cache(CorePlayerTeamRelation relation) {
        relations.put(new ImmutablePair<>(relation.getPlayer(), relation.getTeam()), relation);
    }

    public void forceUnCache(CorePlayer player) {
        playerNameToIdCache.invalidate(player.getName());
        playersCache.invalidate(player.getUuid());
    }

    public void forceUnCache(CoreTeam team) {
        teamNameToIdCache.invalidate(team.getName());
        teamsCache.invalidate(team.getId());
    }

}
