package space.cubicworld.core.model;

import lombok.Builder;
import lombok.Data;
import net.kyori.adventure.text.format.TextColor;
import space.cubicworld.core.CoreDataValue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@Data
@Builder
public class CorePlayer {

    public static CorePlayer defaultPlayer(UUID uuid, String name) {
        return CorePlayer.builder()
                .uuid(uuid)
                .name(name)
                .build();
    }

    public static CorePlayer fromSQL(ResultSet resultSet) throws SQLException {
        return CorePlayer
                .builder()
                .uuid(new UUID(
                        resultSet.getLong(1),
                        resultSet.getLong(2)
                ))
                .globalColor(CoreDataValue.getColor(resultSet.getInt(3)))
                .overworldColor(CoreDataValue.getColor(resultSet.getInt(4)))
                .netherColor(CoreDataValue.getColor(resultSet.getInt(5)))
                .endColor(CoreDataValue.getColor(resultSet.getInt(6)))
                .name(resultSet.getString(7))
                .build();
    }

    private final UUID uuid;
    private final String name;
    private TextColor globalColor;
    private TextColor overworldColor;
    private TextColor netherColor;
    private TextColor endColor;

}
