package space.cubicworld.core;

import lombok.Getter;
import org.slf4j.Logger;
import space.cubicworld.core.color.CoreColorIndexContainer;
import space.cubicworld.core.database.CoreDatabase;
import space.cubicworld.core.database.nocache.CoreNoCacheDatabase;

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
            boolean mysqlSsl,
            ClassLoader classLoader,
            CoreResolver resolver,
            Map<String, String> colors
    ) {
        database = new CoreNoCacheDatabase(
                mysqlHost,
                mysqlUsername,
                mysqlPassword,
                mysqlDatabase,
                mysqlSsl,
                classLoader,
                resolver
        );
        colorIndexContainer = new CoreColorIndexContainer(colors);
    }

}
