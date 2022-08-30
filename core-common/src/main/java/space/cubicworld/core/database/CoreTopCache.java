package space.cubicworld.core.database;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import space.cubicworld.core.CoreStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
class CoreTopCache {

    private static class CoreTop<T> {

        interface Functions<T> {

            /**
             * Fetches objects from database and insert it into array. Returns real length
             *
             * @param array Array
             * @return Real length
             */
            int fetch(T[] array);

            int getWeight(T object);

        }

        private final Functions<T> functions;
        private final T[] array;
        private int maxLength;
        private int realLength;

        public CoreTop(Functions<T> functions, T[] array) {
            this.functions = functions;
            this.array = array;
            maxLength = functions.fetch(this.array);
            realLength = maxLength;
        }

        public void update(T object) {
            int weight = functions.getWeight(object);
            synchronized (this) {
                // If maxLength != topSize then we have all objects, so we do not need to fetch it
                if (array.length >= realLength && maxLength == array.length) {
                    forceUpdateUnSynchronized();
                }
                int previousPlace = -1;
                int newPlace = -1;
                for (int i = 0; i < maxLength; ++i) {
                    T currentChecked = array[i];
                    if (currentChecked == null || (previousPlace != -1 && newPlace != -1)) break;
                    if (currentChecked.equals(object)) previousPlace = i;
                    int currentCheckedWeight = functions.getWeight(currentChecked);
                    if (newPlace == -1 && currentCheckedWeight <= weight) newPlace = i;
                }
                if (previousPlace == newPlace && previousPlace != -1) return;
                if (maxLength != array.length && previousPlace == -1 && newPlace == -1) {
                    array[maxLength++] = object;
                    realLength = maxLength;
                    return;
                }
                if (maxLength != array.length && newPlace == -1) {
                    if (previousPlace == realLength - 1) return;
                    newPlace = maxLength;
                }
                if (previousPlace == -1 && newPlace == -1) return;
                if (previousPlace != -1 && newPlace != -1) {
                    /*
                    Previous - 3 (index) (it is 3)
                    New - 1 (index) (it is 5)
                    Array - [6, 5, 4, 3, 2, 1]
                    Result - [6, 3, 5, 4, 2, 1]
                     */
                    if (previousPlace > newPlace) {
                        moveRight(newPlace, previousPlace);
                    }
                    /*
                    Previous - 1 (index) (it is 5)
                    New - 3 (index) (it is 3)
                    Array - [6, 5, 4, 3, 2, 1]
                    Result - [6, 4, 3, 5, 2, 1]
                     */
                    else {
                        moveLeft(previousPlace, newPlace);
                    }
                    array[newPlace] = object;
                } else if (previousPlace == -1) { // newPlace != -1
                    /*
                    New - 3 (index) (it is 3)
                    New Object - 7
                    Array - [6, 5, 4, 3, 2, 1]
                    Result - [6, 5, 4, 7, 3, 2] (1 is going out if maxLength == array.length)
                     */
                    if (maxLength < array.length) {
                        moveRight(newPlace, maxLength++);
                        realLength++;
                    } else {
                        moveRight(newPlace, array.length);
                    }
                    array[newPlace] = object;
                } else { // newPlace == -1 && previousPlace != -1
                    /*
                    Previous - 3 (index) (it is 3)
                    Array - [6, 5, 4, 3, 2, 1]
                    Result - [6, 5, 4, 2, 1, null]
                     */
                    moveLeft(previousPlace, maxLength);
                    array[(--realLength) - 1] = null;
                }
            }
        }

        /**
         * Example:
         * - Begin equals to 1
         * - End equals to 3
         * - Array equals to [ 6, 5, 4, 3, 2, 1 ]
         * Result equals to [ 6, 4, 3, 3, 2, 1 ]
         * Algorithm on example:
         * i = 1 -> array[1] = array[2] (5 -> 4)
         * i = 2 -> array[2] = array[3] (4 -> 3)
         * i = 3 == END -> end
         *
         * @param begin Begin
         * @param end   End
         */
        private void moveLeft(int begin, int end) {
            for (int i = begin; i < end; ) {
                array[i] = array[++i];
            }
        }

        /**
         * Example:
         * - Begin equals to 1
         * - End equals to 3
         * - Array equals to [ 6, 5, 4, 3, 2, 1 ]
         * Result equals to [ 6, 5, 5, 4, 2, 1 ]
         * Algorithm on example:
         * i = 3 -> array[3] = array[2] (3 -> 4)
         * i = 2 -> array[2] = array[1] (4 -> 5)
         * i = 1 == BEGIN -> end
         *
         * @param begin Begin
         * @param end   End
         */
        private void moveRight(int begin, int end) {
            for (int i = end; i > begin; ) {
                array[i] = array[--i];
            }
        }

        public List<T> getSlice(int size) {
            if (array.length < size) throw new IllegalArgumentException("size is more than array.length");
            synchronized (this) {
                if (realLength != maxLength && realLength < size) {
                    forceUpdateUnSynchronized();
                }
                return Arrays.stream(array).limit(Math.min(realLength, size)).toList();
            }
        }

        public void forceUpdate() {
            synchronized (this) {
                forceUpdateUnSynchronized();
            }
        }

        private void forceUpdateUnSynchronized() {
            maxLength = functions.fetch(array);
            realLength = maxLength;
        }

    }

    private final LoadingCache<Integer, Integer> teamReputations = CacheBuilder
            .newBuilder()
            .expireAfterAccess(Duration.ofDays(1))
            .maximumSize(500)
            .build(CacheLoader.from(this::teamReputationLoader));
    private final CoreTop<Integer> teamReputationTop;
    private final CoreTop<UUID> playerReputationTop;

    private final CoreDatabaseImpl database;

    public CoreTopCache(CoreDatabaseImpl database) {
        this.database = database;
        teamReputationTop = new CoreTop<>(
                new CoreTop.Functions<>() {
                    @Override
                    @SneakyThrows
                    public int fetch(Integer[] array) {
                        try (Connection connection = database.getConnection();
                             PreparedStatement statement = connection.prepareStatement("""
                                     SELECT relation.team_id, SUM(DISTINCT player.reputation)
                                     FROM team_player_relations relation
                                     INNER JOIN players player WHERE player.uuid = relation.player_uuid AND relation.relation = "MEMBERSHIP"
                                     GROUP BY relation.team_id
                                     ORDER BY SUM(DISTINCT player.reputation) DESC
                                     LIMIT ?
                                     """
                             )
                        ) {
                            statement.setInt(1, array.length);
                            ResultSet resultSet = statement.executeQuery();
                            int i = 0;
                            while (resultSet.next()) {
                                int loadedTeamId = resultSet.getInt(1);
                                int loadedReputation = resultSet.getInt(2);
                                teamReputations.put(loadedTeamId, loadedReputation);
                                array[i++] = loadedTeamId;
                            }
                            resultSet.close();
                            return i;
                        }
                    }

                    @Override
                    @SneakyThrows
                    public int getWeight(Integer object) {
                        return teamReputations.get(object);
                    }
                },
                new Integer[CoreStatic.TOP_SIZE * 3]
        );
        playerReputationTop = new CoreTop<>(
                new CoreTop.Functions<>() {
                    @Override
                    @SneakyThrows
                    public int fetch(UUID[] array) {
                        List<CorePlayer> players = database
                                .fetchPlayers(
                                        "SELECT * FROM players ORDER BY reputation DESC LIMIT ?",
                                        array.length
                                );
                        int counter = 0;
                        for (CorePlayer player: players) array[counter++] = player.getId();
                        return players.size();
                    }

                    @Override
                    public int getWeight(UUID object) {
                        return database.fetchPlayer(object)
                                .orElseThrow()
                                .getReputation();
                    }
                },
                new UUID[CoreStatic.TOP_SIZE * 3]
        );
    }

    @SneakyThrows
    private int teamReputationLoader(int teamId) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT SUM(player.reputation)
                     FROM team_player_relations relation
                     INNER JOIN players player ON player.uuid = relation.player_uuid AND relation.relation = MEMBERSHIP
                     WHERE relation.team_id = ?
                     """)
        ) {
            statement.setInt(1, teamId);
            ResultSet resultSet = statement.executeQuery();
            int reputation = resultSet.next() ? resultSet.getInt(1) : 0;
            resultSet.close();
            return reputation;
        }
    }

    public List<Integer> getTeamReputationTop() {
        return teamReputationTop.getSlice(CoreStatic.TOP_SIZE);
    }

    public List<UUID> getPlayerReputationTop() {
        return playerReputationTop.getSlice(CoreStatic.TOP_SIZE);
    }

    public void updateReputation(UUID playerId, int offset) {
        database.getRelationCache()
                .fetchTeams(CorePTRelation.Value.MEMBERSHIP, playerId, CoreRelationCache.ALL)
                .forEach(teamId -> database.fetchTeam(teamId)
                        .ifPresent(team -> {
                            teamReputations.put(
                                    teamId,
                                    teamReputations.getUnchecked(teamId) + offset
                            );
                            teamReputationTop.update(teamId);
                        })
                );
        playerReputationTop.update(playerId);
    }

    public void newTeam(int teamId) {
        teamReputations.put(teamId, 0);
        teamReputationTop.update(teamId);
    }

    public void newPlayer(UUID playerId) {
        playerReputationTop.update(playerId);
    }

    public int getTeamReputation(int teamId) {
        return teamReputations.getUnchecked(teamId);
    }

}
