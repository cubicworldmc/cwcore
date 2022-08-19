package space.cubicworld.core;

import lombok.Getter;
import org.slf4j.Logger;
import space.cubicworld.core.color.CoreColorIndexContainer;
import space.cubicworld.core.database.CoreDatabase;
import space.cubicworld.core.database.CoreDatabaseImpl;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

@Getter
public class CorePlugin {

    private final CoreDatabase database;
    private final CoreColorIndexContainer colorIndexContainer;

    public CorePlugin(
            String mysqlHost,
            String mysqlUsername,
            String mysqlPassword,
            String mysqlDatabase,
            ClassLoader loader,
            Logger logger,
            CoreResolver resolver,
            Map<String, String> colors
    ) throws ClassNotFoundException, SQLException, IOException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        database = new CoreDatabaseImpl(
                mysqlHost,
                mysqlUsername,
                mysqlPassword,
                mysqlDatabase,
                loader,
                logger,
                resolver
        );
        colorIndexContainer = new CoreColorIndexContainer(colors);
    }

}
