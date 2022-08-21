package space.cubicworld.core.database;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@RequiredArgsConstructor
class CoreBoostCache {

    public static final long MONTH = Duration.ofDays(30).toMillis();

    private static CacheBuilder<Object, Object> defaultBuilder() {
        return CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterAccess(Duration.ofDays(1));
    }

    private final Map<Long, Object> locks = new ConcurrentHashMap<>();

    private final LoadingCache<Long, Optional<CoreBoost>> boosts = defaultBuilder()
            .build(CacheLoader.from(this::boostLoader));
    private final LoadingCache<Integer, Set<Long>> teamBoosts = defaultBuilder()
            .build(CacheLoader.from(this::teamBoostsLoader));
    private final LoadingCache<UUID, Set<Long>> playerBoosts = defaultBuilder()
            .build(CacheLoader.from(this::playerBoostsLoader));

    private final CoreDatabaseImpl database;

    private Object computeLock(Object key) {
        return new Object[0];
    }

    @SneakyThrows
    private Optional<CoreBoost> boostLoader(long id) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT boost.player_uuid, boost.ends, team_boost.boost_id FROM player_boosts boost
                     RIGHT JOIN team_boosts team_boost ON team_boost.boost_id = boost.id
                     WHERE id = ?
                     """
             )) {
            synchronized (locks.computeIfAbsent(id, this::computeLock)) {
                statement.setLong(1, id);
                ResultSet resultSet = statement.executeQuery();
                CoreBoostImpl boost = new CoreBoostImpl(
                        database,
                        id,
                        UUID.fromString(resultSet.getString(1))
                );
                boost.setEnd(resultSet.getLong(2));
                Integer teamId = resultSet.getObject(3, Integer.class);
                if (teamId != null) boost.setTeamId(teamId);
                resultSet.close();
                if (boost.isEnd()) return Optional.empty();
                return Optional.of(boost);
            }
        }
    }

    @SneakyThrows
    private Set<Long> teamBoostsLoader(int team) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT boost.id, boost.player_uuid, boost.ends FROM player_boosts boost
                     INNER JOIN team_boosts team_boost ON team_boost.team_id = ? AND team_boost.boost_id = boost.id
                     """
             )) {
            statement.setInt(1, team);
            ResultSet resultSet = statement.executeQuery();
            List<Long> result = new ArrayList<>();
            while (resultSet.next()) {
                CoreBoostImpl boost = new CoreBoostImpl(
                        database,
                        resultSet.getLong(1),
                        UUID.fromString(resultSet.getString(2))
                );
                boost.setEnd(resultSet.getLong(3));
                if (boost.isEnd()) remove(boost);
                cache(boost);
                result.add(boost.getId());
            }
            resultSet.close();
            return new CopyOnWriteArraySet<>(result);
        }
    }

    @SneakyThrows
    private Set<Long> playerBoostsLoader(UUID player) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, ends FROM player_boosts WHERE player_uuid = ?
                     """)) {
            statement.setString(1, player.toString());
            ResultSet resultSet = statement.executeQuery();
            List<Long> result = new ArrayList<>();
            while (resultSet.next()) {
                CoreBoostImpl boost = new CoreBoostImpl(
                        database,
                        resultSet.getLong(1),
                        player
                );
                boost.setEnd(resultSet.getLong(2));
                if (boost.isEnd()) remove(boost);
                cache(boost);
                result.add(boost.getId());
            }
            resultSet.close();
            return new CopyOnWriteArraySet<>(result);
        }
    }

    public CoreBoost cache(CoreBoost boost) {
        Optional<CoreBoost> result;
        if ((result = boosts.getIfPresent(boost.getId())) == null || result.isEmpty()) {
            synchronized (locks.computeIfAbsent(boost.getId(), this::computeLock)) {
                if ((result = boosts.getIfPresent(boost.getId())) == null || result.isEmpty()) {
                    boosts.put(boost.getId(), Optional.of(boost));
                    Set<Long> ids = playerBoosts.getIfPresent(boost.getPlayerId());
                    if (ids != null) ids.add(boost.getId());
                    result = Optional.of(boost);
                }
            }
        }
        return result.orElseThrow();
    }

    @SneakyThrows
    public Optional<CoreBoost> fetchBoost(long id) {
        return boosts.get(id);
    }

    @SneakyThrows
    public List<CoreBoost> fetchTeamBoosts(int team) {
        return teamBoosts.get(team)
                .stream()
                .map(id -> fetchBoost(id).orElseThrow())
                .toList();
    }

    @SneakyThrows
    public List<CoreBoost> fetchPlayerBoosts(UUID player) {
        return playerBoosts.get(player)
                .stream()
                .map(id -> fetchBoost(id).orElseThrow())
                .toList();
    }

    public Set<Long> getTeamBoostsIfPresent(int team) {
        return teamBoosts.getIfPresent(team);
    }

    public Set<Long> getPlayerBoostsIfPresent(UUID player) {
        return playerBoosts.getIfPresent(player);
    }

    public void update(CoreBoost boost) throws SQLException {
        Integer teamId = boost.getTeamId();
        try (Connection connection = database.getConnection();
             PreparedStatement updateStatement = connection.prepareStatement(
                     "UPDATE player_boosts SET ends = ? WHERE id = ?"
             );
             PreparedStatement teamUpdateStatement = connection.prepareStatement(teamId != null ?
                     "INSERT INTO team_boosts (boost_id, team_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE team_id = ?" :
                     "DELETE FROM team_boosts WHERE boost_id = ?"
             )
        ) {
            updateStatement.setLong(1, boost.getEnd());
            updateStatement.setLong(2, boost.getId());
            teamUpdateStatement.setLong(1, boost.getId());
            if (teamId != null) {
                teamUpdateStatement.setInt(2, teamId);
                teamUpdateStatement.setInt(3, teamId);
            }
            updateStatement.executeUpdate();
            teamUpdateStatement.executeUpdate();
        }
    }

    public void remove(CoreBoost boost) throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM player_boosts WHERE id = ?"
             )) {
            statement.setLong(1, boost.getId());
            statement.executeUpdate();
        }
    }

}
