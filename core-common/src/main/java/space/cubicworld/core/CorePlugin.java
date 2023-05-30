package space.cubicworld.core;

import lombok.Getter;
import lombok.experimental.Delegate;
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
    @Delegate
    private final CoreBootstrap bootstrap;

    public CorePlugin(CoreBootstrap bootstrap) {
        this.bootstrap = bootstrap;
        database = new CoreNoCacheDatabase(this);
        colorIndexContainer = new CoreColorIndexContainer(this);
    }

}
