package space.cubicworld.core;

import lombok.Builder;
import lombok.Data;
import net.kyori.adventure.text.format.TextColor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@Data
@Builder
public class CorePlayer {

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
                .build();
    }

    private final UUID uuid;
    private TextColor globalColor;
    private TextColor overworldColor;
    private TextColor netherColor;
    private TextColor endColor;

}
