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
import java.util.UUID;

@RequiredArgsConstructor
class CoreTopCache {

    private final LoadingCache<Integer, Integer> teamReputations = CacheBuilder
            .newBuilder()
            .expireAfterAccess(Duration.ofDays(1))
            .maximumSize(500)
            .build(CacheLoader.from(this::teamReputationLoader));
    private final Integer[] teamReputationTop = new Integer[CoreStatic.TOP_SIZE];
    private final UUID[] playerReputationTop = new UUID[CoreStatic.TOP_SIZE];

    private final CoreDatabaseImpl database;

    @SneakyThrows
    private int teamReputationLoader(int teamId) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT SUM(player.reputation)
                     FROM team_player_relations relation
                     INNER JOIN players player ON player.uuid = relation.player_uuid
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

    @SneakyThrows
    public void setTeamReputation(int teamId, int reputation) {

    }

    @SneakyThrows
    public void setPlayerReputation(UUID playerId, int reputation) {

    }

}
