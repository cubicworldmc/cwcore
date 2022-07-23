package space.cubicworld.core.model;

import lombok.Builder;
import lombok.Data;
import net.kyori.adventure.text.format.TextColor;

import java.sql.SQLException;
import java.util.UUID;

@Data
@Builder
public final class CorePlayer {

    public static CorePlayer read(CoreResult result) throws SQLException {
        return CorePlayer.builder()
                .uuid(new UUID(result.readRow(), result.readRow()))
                .name(result.readRow())
                .globalColor(CorePrimitive.toColor(result.readRow()))
                .reputation(result.readRow())
                .build();
    }

    public static CoreStatement<CorePlayer> insertStatement() {
        return CoreStatement.<CorePlayer>builder()
                .sql("INSERT IGNORE INTO players (uuid_most, uuid_least, name) VALUES (?, ?, ?)")
                .parameter(player -> player.getUuid().getMostSignificantBits())
                .parameter(player -> player.getUuid().getLeastSignificantBits())
                .parameter(CorePlayer::getName)
                .build();
    }

    public static CoreStatement<CorePlayer> updateStatement() {
        return CoreStatement.<CorePlayer>builder()
                .sql("""
                        UPDATE players SET
                        global_color = ?, reputation = ?, name = ?
                        WHERE uuid_most = ? AND uuid_least = ?
                        """)
                .parameter(player -> CorePrimitive.toSQL(player.getGlobalColor()))
                .parameter(CorePlayer::getReputation)
                .parameter(CorePlayer::getName)
                .parameter(player -> player.getUuid().getMostSignificantBits())
                .parameter(player -> player.getUuid().getLeastSignificantBits())
                .build();
    }

    public static CoreStatement<UUID> selectByUuidStatement() {
        return CoreStatement.<UUID>builder()
                .sql("SELECT * FROM players WHERE uuid_most = ? AND uuid_least = ?")
                .parameter(UUID::getMostSignificantBits)
                .parameter(UUID::getLeastSignificantBits)
                .build();
    }

    public static CoreStatement<String> selectByNameStatement() {
        return CoreStatement.<String>builder()
                .sql("SELECT * FROM players WHERE name = ?")
                .parameter(name -> name)
                .build();
    }

    private final UUID uuid;
    private String name;
    private TextColor globalColor;
    private int reputation;

}
