package space.cubicworld.core.database;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;
import lombok.*;
import space.cubicworld.core.util.ImmutablePair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

class CoreRelationCache {

    public static final int ALL = -1;

    @Getter
    @ToString
    static class RelationCacheByValueSet<T> {

        public static final int UNKNOWN_SIZE = -1;

        private final Object lock = new Object[0];

        private final Set<T> set = Collections.synchronizedSet(new HashSet<>());
        @Setter(onMethod_ = @Synchronized("lock"))
        @Getter(onMethod_ = @Synchronized("lock"))
        private int size = UNKNOWN_SIZE;

        public void addObject(T object) {
            set.add(object);
            incrementSize();
        }

        public void removeObject(T object) {
            set.remove(object);
            decrementSize();
        }

        public void incrementSize() {
            synchronized (lock) {
                if (size != UNKNOWN_SIZE) size += 1;
            }
        }

        public void decrementSize() {
            synchronized (lock) {
                if (size != UNKNOWN_SIZE) size -= 1;
            }
        }

        public boolean isAll() {
            return set.size() == size;
        }

    }

    @Getter
    static class RelationCacheByValue {
        private final Map<UUID, RelationCacheByValueSet<Integer>> teams = new ConcurrentHashMap<>();
        private final Map<Integer, RelationCacheByValueSet<UUID>> players = new ConcurrentHashMap<>();

        public void remove(UUID player, int team) {
            RelationCacheByValueSet<Integer> teamSet = teams.get(player);
            if (teamSet != null) {
                teamSet.removeObject(team);
            }
            RelationCacheByValueSet<UUID> playerSet = players.get(team);
            if (playerSet != null) {
                playerSet.removeObject(player);
            }
        }

        public void unCache(UUID player, int team) {
            RelationCacheByValueSet<Integer> teamSet = teams.get(player);
            if (teamSet != null) {
                teamSet.set.remove(team);
            }
            RelationCacheByValueSet<UUID> playerSet = players.get(team);
            if (playerSet != null) {
                playerSet.set.remove(player);
            }
        }

        public void cache(UUID player, int team) {
            teams.computeIfAbsent(player, key -> new RelationCacheByValueSet<>()).set.add(team);
            players.computeIfAbsent(team, key -> new RelationCacheByValueSet<>()).set.add(player);
        }

        public void put(UUID player, int team) {
            teams.computeIfAbsent(player, key -> new RelationCacheByValueSet<>()).addObject(team);
            players.computeIfAbsent(team, key -> new RelationCacheByValueSet<>()).addObject(player);
        }
    }

    private final LoadingCache<ImmutablePair<UUID, Integer>, CorePTRelation> relations =
            CacheBuilder
                    .newBuilder()
                    .maximumSize(10000)
                    .expireAfterAccess(Duration.ofHours(1))
                    .removalListener(this::removalListener)
                    .build(CacheLoader.from(this::relationLoader));
    private final Map<CorePTRelation.Value, RelationCacheByValue> byValueCache = new ConcurrentHashMap<>();

    private final CoreDatabaseImpl database;

    public CoreRelationCache(CoreDatabaseImpl database) {
        this.database = database;
        for (CorePTRelation.Value value : CorePTRelation.Value.values()) {
            byValueCache.put(value, new RelationCacheByValue());
        }
    }

    private void removalListener(RemovalNotification<ImmutablePair<UUID, Integer>, CorePTRelation> notification) {
        UUID player = notification.getKey().getFirst();
        int team = notification.getKey().getSecond();
        CorePTRelation.Value value = notification.getValue().getValue();
        RelationCacheByValue byValue = byValueCache.get(value);
        byValue.unCache(player, team);
    }

    @SneakyThrows
    private CorePTRelation relationLoader(ImmutablePair<UUID, Integer> key) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT relation FROM team_player_relations WHERE player_uuid = ? AND team_id = ?
                     """)) {
            statement.setString(1, key.getFirst().toString());
            statement.setInt(2, key.getSecond());
            ResultSet resultSet = statement.executeQuery();
            CorePTRelation relation = new CorePTRelationImpl(
                    database,
                    key.getFirst(),
                    key.getSecond()
            );
            relation.setValue(
                    resultSet.next() ?
                            CorePTRelation.Value.valueOf(resultSet.getString(1)) :
                            CorePTRelation.Value.NONE
            );
            resultSet.close();
            byValueCache.get(relation.getValue()).cache(key.getFirst(), key.getSecond());
            return relation;
        }
    }

    @SneakyThrows
    public CorePTRelation fetchRelation(UUID player, int team) {
        return relations.get(new ImmutablePair<>(player, team));
    }

    @SneakyThrows
    public Collection<Integer> fetchTeams(CorePTRelation.Value value, UUID player, int count) {
        return fetch(
                value, player, count,
                byValueCache.get(value).getTeams().computeIfAbsent(player, key -> new RelationCacheByValueSet<>()),
                o -> (int) o,
                key -> cacheIfNeed(player, key, value),
                "SELECT team_id FROM team_player_relations WHERE relation = ? AND player_uuid = ?"
        );
    }

    @SneakyThrows
    public Collection<UUID> fetchPlayers(CorePTRelation.Value value, int team, int count) {
        return fetch(
                value, team, count,
                byValueCache.get(value).getPlayers().computeIfAbsent(team, key -> new RelationCacheByValueSet<>()),
                o -> UUID.fromString(o.toString()),
                key -> cacheIfNeed(key, team, value),
                "SELECT player_uuid FROM team_player_relations WHERE relation = ? AND team_id = ?"
        );
    }

    private <T, V> Collection<T> fetch(
            CorePTRelation.Value value,
            V key,
            int count,
            RelationCacheByValueSet<T> cacheByValueSet,
            Function<Object, T> parseFunction,
            Consumer<T> cache,
            String sql
    ) throws SQLException {
        if (value == CorePTRelation.Value.NONE) {
            throw new IllegalArgumentException("None value is not supported");
        }
        if (count != ALL) {
            if (cacheByValueSet.set.size() >= count) {
                return cacheByValueSet.set.stream().limit(count).toList();
            }
        }
        if (cacheByValueSet.isAll()) {
            return Collections.unmodifiableSet(cacheByValueSet.set);
        }
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql + (count == ALL ? "" : " LIMIT ?")
             )
        ) {
            statement.setString(1, value.name());
            statement.setObject(2, key instanceof UUID ? key.toString() : key);
            if (count != ALL) statement.setInt(3, count);
            ResultSet resultSet = statement.executeQuery();
            List<T> result = new ArrayList<>();
            while (resultSet.next()) {
                T resultKey = parseFunction.apply(resultSet.getObject(1));
                cache.accept(resultKey);
                result.add(resultKey);
            }
            System.out.println(result);
            resultSet.close();
            if (count == ALL || result.size() < count) cacheByValueSet.setSize(result.size());
            return Collections.unmodifiableList(result);
        }
    }

    @SneakyThrows
    public int fetchTeamsSize(CorePTRelation.Value value, UUID player) {
        return fetchSize(
                value,
                player,
                byValueCache
                        .get(value)
                        .getTeams()
                        .computeIfAbsent(player, key -> new RelationCacheByValueSet<>()),
                "SELECT COUNT(*) FROM team_player_relations WHERE relation = ? AND player_uuid = ?"
        );
    }

    @SneakyThrows
    public int fetchPlayersSize(CorePTRelation.Value value, int team) {
        return fetchSize(
                value,
                team,
                byValueCache
                        .get(value)
                        .getPlayers()
                        .computeIfAbsent(team, key -> new RelationCacheByValueSet<>()),
                "SELECT COUNT(*) FROM team_player_relations WHERE relation = ? AND team_id = ?"
        );
    }

    private <T, V> int fetchSize(
            CorePTRelation.Value value,
            V key,
            RelationCacheByValueSet<T> cacheSet,
            String sql
    ) throws SQLException {
        if (cacheSet.isAll()) return cacheSet.getSize();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value.name());
            statement.setObject(2, key instanceof UUID ? key.toString() : key);
            ResultSet resultSet = statement.executeQuery();
            int count = resultSet.next() ? resultSet.getInt(1) : 0;
            resultSet.close();
            cacheSet.setSize(count);
            return count;
        }
    }

    private void cacheIfNeed(UUID player, int team, CorePTRelation.Value value) {
        ImmutablePair<UUID, Integer> key = new ImmutablePair<>(player, team);
        if (relations.getIfPresent(key) == null) {
            CorePTRelation relation = new CorePTRelationImpl(
                    database,
                    player,
                    team
            );
            relation.setValue(value);
            byValueCache.computeIfAbsent(value, it -> new RelationCacheByValue()).cache(player, team);
            relations.put(new ImmutablePair<>(player, team), relation);
        }
    }

    public void changeRelationValue(
            UUID player, int team,
            CorePTRelation.Value oldValue,
            CorePTRelation.Value newValue
    ) {
        byValueCache.get(oldValue).remove(player, team);
        byValueCache.get(newValue).put(player, team);
    }

}
