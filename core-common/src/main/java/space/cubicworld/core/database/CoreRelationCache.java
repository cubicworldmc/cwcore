package space.cubicworld.core.database;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.Synchronized;
import space.cubicworld.core.util.ImmutablePair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class CoreRelationCache {

    public static final int ALL = -1;

    @Getter
    static class RelationCacheByValueSet<T> {
        private final Set<T> set = Collections.synchronizedSet(new HashSet<>());
        @Setter(onMethod_ = @Synchronized)
        @Getter(onMethod_ = @Synchronized)
        private boolean all;
    }

    @Getter
    static class RelationCacheByValue {
        private final Map<UUID, RelationCacheByValueSet<Integer>> teams = new ConcurrentHashMap<>();
        private final Map<Integer, RelationCacheByValueSet<UUID>> players = new ConcurrentHashMap<>();

        public void remove(UUID player, int team) {
            RelationCacheByValueSet<Integer> teamSet = teams.get(player);
            if (teamSet != null) teamSet.set.remove(team);
            RelationCacheByValueSet<UUID> playerSet = players.get(team);
            if (playerSet != null) playerSet.set.remove(player);
        }

        public void unCache(UUID player, int team) {
            RelationCacheByValueSet<Integer> teamSet = teams.get(player);
            if (teamSet != null) {
                teamSet.set.remove(team);
                teamSet.setAll(false);
            }
            RelationCacheByValueSet<UUID> playerSet = players.get(team);
            if (playerSet != null) {
                playerSet.set.remove(player);
                playerSet.setAll(false);
            }
        }

        public void put(UUID player, int team) {
            teams.computeIfAbsent(player, key -> new RelationCacheByValueSet<>()).set.add(team);
            players.computeIfAbsent(team, key -> new RelationCacheByValueSet<>()).set.add(player);
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
            byValueCache.get(relation.getValue()).put(key.getFirst(), key.getSecond());
            return relation;
        }
    }

    @SneakyThrows
    public CorePTRelation fetchRelation(UUID player, int team) {
        return relations.get(new ImmutablePair<>(player, team));
    }

    @SneakyThrows
    public Collection<Integer> fetchTeams(CorePTRelation.Value value, UUID player, int count) {
        RelationCacheByValueSet<Integer> teamsSet = byValueCache.get(value).getTeams().get(player);
        if (count != ALL) {
            if (teamsSet != null && teamsSet.set.size() >= count) {
                return teamsSet.set.stream().limit(count).toList();
            }
        }
        else if (teamsSet.isAll()){
            return Collections.unmodifiableCollection(teamsSet.set);
        }
        try (Connection connection = database.getConnection();
            PreparedStatement statement = connection.prepareStatement("""
                    SELECT team_id FROM team_player_relations WHERE relation = ? AND player_uuid = ?
                    """ + (count == ALL ? "" : " LIMIT ?")
            )) {
            statement.setString(1, value.name());
            statement.setString(2, player.toString());
            if (count != ALL) statement.setInt(3, count);
            ResultSet resultSet = statement.executeQuery();
            List<Integer> teams = new ArrayList<>();
            while (resultSet.next()) {
                int team = resultSet.getInt(1);
                cacheIfNeed(player, team, value);
                teams.add(team);
            }
            resultSet.close();
            RelationCacheByValueSet<Integer> newTeamSet = byValueCache.get(value).getTeams().get(player);
            if (newTeamSet != null) newTeamSet.setAll(count == ALL);
            return Collections.unmodifiableCollection(teams);
        }
    }

    @SneakyThrows
    public Collection<UUID> fetchPlayers(CorePTRelation.Value value, int team, int count) {
        RelationCacheByValueSet<UUID> playerSet = byValueCache.get(value).getPlayers().get(team);
        if (count != ALL) {
            if (playerSet != null && playerSet.set.size() >= count) {
                return playerSet.set.stream().limit(count).toList();
            }
        }
        if (playerSet != null && playerSet.isAll()){
            return Collections.unmodifiableCollection(playerSet.set);
        }
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                    SELECT player_uuid FROM team_player_relations WHERE relation = ? AND team_id = ?
                    """ + (count == ALL ? "" : " LIMIT ?")
             )) {
            statement.setString(1, value.name());
            statement.setInt(2, team);
            if (count != ALL) statement.setInt(3, count);
            ResultSet resultSet = statement.executeQuery();
            List<UUID> players = new ArrayList<>();
            while (resultSet.next()) {
                UUID player = UUID.fromString(resultSet.getString(1));
                cacheIfNeed(player, team, value);
                players.add(player);
            }
            resultSet.close();
            RelationCacheByValueSet<UUID> newPlayerSet = byValueCache.get(value).getPlayers().get(team);
            if (newPlayerSet != null) newPlayerSet.setAll(count == ALL);
            return Collections.unmodifiableCollection(players);
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
            byValueCache.get(value).put(player, team);
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
