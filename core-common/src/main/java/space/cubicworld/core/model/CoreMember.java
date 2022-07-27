package space.cubicworld.core.model;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CoreMember {

    public static CoreStatement<Integer> selectMembersByTeamIdStatement() {
        return CoreStatement.<Integer>builder()
                .sql("""
                        SELECT
                        player.uuid_most, player.uuid_least, player.name, player.global_color, player.reputation
                        FROM team_members member
                        INNER JOIN players player ON
                        player.uuid_most = member.uuid_most AND player.uuid_least = member.uuid_least
                        WHERE member.id = ?
                        """)
                .parameter(id -> id)
                .build();
    }

    public static CoreStatement<UUID> selectTeamsByPlayerStatement() {
        return CoreStatement.<UUID>builder()
                .sql("""
                        SELECT
                        team.id, team.name, team.description, team.owner_uuid_most, team.owner_uuid_least
                        FROM team_members member
                        INNER JOIN teams team ON
                        team.id = member.id
                        WHERE member.uuid_most = ? AND member.uuid_least = ?
                        """)
                .parameter(UUID::getMostSignificantBits)
                .parameter(UUID::getLeastSignificantBits)
                .build();
    }

    public static CoreStatement<CoreMember> insertStatement() {
        return CoreStatement.<CoreMember>builder()
                .sql("INSERT INTO team_members (id, uuid_most, uuid_least) VALUES (?, ?, ?)")
                .parameter(CoreMember::getId)
                .parameter(member -> member.getUuid().getMostSignificantBits())
                .parameter(member -> member.getUuid().getLeastSignificantBits())
                .build();
    }

    public static CoreStatement<CoreMember> deleteStatement() {
        return CoreStatement.<CoreMember>builder()
                .sql("DELETE FROM team_members WHERE id = ? AND uuid_most = ? AND uuid_least = ?")
                .parameter(CoreMember::getId)
                .parameter(member -> member.getUuid().getMostSignificantBits())
                .parameter(member -> member.getUuid().getLeastSignificantBits())
                .build();
    }

    private final int id;
    private final UUID uuid;

}
