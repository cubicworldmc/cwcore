package space.cubicworld.core.model;

import lombok.Builder;
import lombok.Data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@Data
@Builder
public class CoreTeam {

    public static CoreTeam fromSQL(ResultSet resultSet) throws SQLException {
        return CoreTeam
                .builder()
                .id(resultSet.getInt(1))
                .name(resultSet.getString(2))
                .description(resultSet.getString(3))
                .owner(new UUID(
                        resultSet.getLong(4),
                        resultSet.getLong(5)
                ))
                .membersVisible(resultSet.getBoolean(6))
                .build();
    }

    private final int id;
    private String name;
    private String description;
    private UUID owner;
    private boolean membersVisible;

}
