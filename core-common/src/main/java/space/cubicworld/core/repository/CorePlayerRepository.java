package space.cubicworld.core.repository;

import space.cubicworld.core.model.CorePlayer;
import space.cubicworld.core.CoreDataValue;
import space.cubicworld.core.database.DatabaseModule;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class CorePlayerRepository {

    private final DatabaseModule database;

    public CorePlayerRepository(DatabaseModule database) {
        this.database = database;
    }

    public Optional<CorePlayer> findPlayer(UUID uuid) throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement selectStatement = connection.prepareStatement("""
                     SELECT * FROM players WHERE uuid_most = ? AND uuid_least = ?
                     """)
        ) {
            selectStatement.setLong(1, uuid.getMostSignificantBits());
            selectStatement.setLong(2, uuid.getLeastSignificantBits());
            ResultSet resultSet = selectStatement.executeQuery();
            if (resultSet.next()) {
                return Optional.of(CorePlayer.fromSQL(resultSet));
            }
            return Optional.empty();
        }
    }

    public void updatePlayer(CorePlayer player) throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement upsertStatement = connection.prepareStatement("""
                     INSERT INTO players(
                         uuid_most,
                         uuid_least,
                         global_color,
                         overworld_color,
                         nether_color,
                         end_color
                     ) VALUES (?, ?, ?, ?, ?, ?)
                     ON DUPLICATE KEY UPDATE
                        global_color = ?,
                        overworld_color = ?,
                        nether_color = ?,
                        end_color = ?
                     """)
        ) {
            long uuidMost = player.getUuid().getMostSignificantBits();
            long uuidLeast = player.getUuid().getLeastSignificantBits();
            int globalColor = CoreDataValue.toValue(player.getGlobalColor());
            int overworldColor = CoreDataValue.toValue(player.getOverworldColor());
            int netherColor = CoreDataValue.toValue(player.getNetherColor());
            int endColor = CoreDataValue.toValue(player.getEndColor());
            upsertStatement.setLong(1, uuidMost);
            upsertStatement.setLong(2, uuidLeast);
            Object[] values = new Object[]{globalColor, overworldColor, netherColor, endColor};
            int current = 3;
            for (Object value : values) {
                upsertStatement.setObject(current, value);
                upsertStatement.setObject((current++) + values.length, value);
            }
            upsertStatement.executeUpdate();
        }
    }

    public void insertDefault(UUID uuid) throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement insertDefaultStatement = connection.prepareStatement("""
                     INSERT IGNORE INTO players (uuid_most, uuid_least) VALUES (?,?)
                     """)
        ) {
            insertDefaultStatement.setLong(1, uuid.getMostSignificantBits());
            insertDefaultStatement.setLong(2, uuid.getLeastSignificantBits());
            insertDefaultStatement.executeUpdate();
        }
    }

}
