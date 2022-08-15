package space.cubicworld.core;

import lombok.Getter;
import org.slf4j.Logger;
import space.cubicworld.core.database.CoreDatabase;
import space.cubicworld.core.database.CoreDatabaseImpl;

import java.io.IOException;
import java.sql.SQLException;

@Getter
public class CorePlugin {

    private final CoreDatabase database;

    public CorePlugin(
            String mysqlHost,
            String mysqlUsername,
            String mysqlPassword,
            String mysqlDatabase,
            ClassLoader loader,
            Logger logger
    ) throws ClassNotFoundException, SQLException, IOException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        database = new CoreDatabaseImpl(
                mysqlHost,
                mysqlUsername,
                mysqlPassword,
                mysqlDatabase,
                loader,
                logger
        );
    }

}
