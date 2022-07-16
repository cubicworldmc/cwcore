package space.cubicworld.core.model;

import lombok.Data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@Data
public class CoreTeamMember {

    public static CoreTeamMember fromSQL(ResultSet resultSet, boolean ownerHad) throws SQLException {
        return new CoreTeamMember(
                resultSet.getInt(1),
                new UUID(
                        resultSet.getLong(2),
                        resultSet.getLong(3)
                ),
                ownerHad ?
                        CoreOwnership.fromBoolean(resultSet.getBoolean(4)) :
                        CoreOwnership.UNDEFINED
        );
    }

    private final int id;
    private final UUID member;
    private final CoreOwnership owner;

}
