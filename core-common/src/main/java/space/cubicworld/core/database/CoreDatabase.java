package space.cubicworld.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import space.cubicworld.core.CorePlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class CoreDatabase {

    private final HikariDataSource dataSource;

    public CoreDatabase(
            String host, String username,
            String password, String database,
            CorePlugin plugin
    ) throws SQLException, ClassNotFoundException, IOException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        HikariConfig config = new HikariConfig();
        try (InputStream hikariPropertiesInputStream = plugin.readResource("hikari.properties")) {
            if (hikariPropertiesInputStream != null) {
                Properties hikariProperties = new Properties();
                hikariProperties.load(hikariPropertiesInputStream);
                config.setDataSourceProperties(hikariProperties);
            }
        } catch (IOException e) {
            plugin.getLogger().warn("Failed to load hikari.properties resource:", e);
        }
        config.setJdbcUrl("jdbc:mysql://%s/%s".formatted(host, database));
        config.setUsername(username);
        config.setPassword(password);
        dataSource = new HikariDataSource(config);
        try (InputStream setupSQLInputStream = plugin.readResource("setup.sql");
             Connection connection = getConnection();
             Statement setupStatement = connection.createStatement();
        ) {
            String setupSQL = new String(setupSQLInputStream.readAllBytes(), StandardCharsets.UTF_8);
            String[] setupSQLBatches = setupSQL.split(";");
            for (String batch: setupSQLBatches) {
                setupStatement.addBatch(batch);
            }
            setupStatement.executeLargeBatch();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

}
