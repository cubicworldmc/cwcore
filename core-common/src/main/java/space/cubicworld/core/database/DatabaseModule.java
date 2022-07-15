package space.cubicworld.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import space.cubicworld.core.CoreStatic;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DatabaseModule {

    private final HikariDataSource dataSource;

    public DatabaseModule(String host, String username,
                          String password, String database, InputStream properties) throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.jdbc.Driver");
        HikariConfig config = new HikariConfig();
        if (properties != null) {
            Properties additionalProperties = new Properties();
            try {
                additionalProperties.load(properties);
                config.setDataSourceProperties(additionalProperties);
            } catch (IOException e) {
                CoreStatic.getLogger().error("IOException while reading additional properties:", e);
            }
        }
        config.setJdbcUrl("jdbc:mysql://%s/%s".formatted(host, database));
        config.setUsername(username);
        config.setPassword(password);
        dataSource = new HikariDataSource(config);
        try (Connection connection = getConnection();
             Statement setupStatement = connection.createStatement();
        ) {
            setupStatement.addBatch("""
                    CREATE TABLE IF NOT EXISTS players (
                        uuid_most BIGINT NOT NULL,
                        uuid_least BIGINT NOT NULL,
                        global_color INT NOT NULL DEFAULT -1,
                        overworld_color INT NOT NULL DEFAULT -1,
                        nether_color INT NOT NULL DEFAULT -1,
                        end_color INT NOT NULL DEFAULT -1,
                        UNIQUE KEY(uuid_most, uuid_least)
                    );
                    """);
            setupStatement.executeBatch();
        }
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

}
