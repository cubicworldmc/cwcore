package space.cubicworld.core.database;

import lombok.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

@Data
public class CorePlayerTeamRelation {

    @Getter
    @RequiredArgsConstructor
    public enum Relation {
        MEMBERSHIP,
        INVITE,
        NONE
    }

    @SneakyThrows
    public static void update(CoreDatabase database, Relation relation, UUID player, int team) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     relation == Relation.NONE ?
                             "DELETE FROM team_player_relations WHERE player_uuid = ? AND team_id = ?" :
                             """
                                     INSERT INTO team_player_relations(player_uuid, team_id, relation)
                                     VALUES (?, ?, ?)
                                     ON DUPLICATE KEY UPDATE relation = ?
                                     """
             )
        ) {
            statement.setString(1, player.toString());
            statement.setInt(2, team);
            if (relation != Relation.NONE) {
                statement.setString(3, relation.name());
                statement.setString(4, relation.name());
            }
            statement.executeUpdate();
        }
    }

    private final UUID player;
    private final int team;
    private final CoreDatabase database;

    @Setter(onMethod_ = @Synchronized)
    @Getter(onMethod_ = @Synchronized)
    private Relation relation;

    public void update() {
        update(database, relation, player, team);
    }

}
