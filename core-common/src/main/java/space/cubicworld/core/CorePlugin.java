package space.cubicworld.core;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Delegate;
import org.slf4j.Logger;
import reactor.netty.http.server.HttpServer;
import space.cubicworld.core.color.CoreColorIndexContainer;
import space.cubicworld.core.database.CoreDatabase;
import space.cubicworld.core.database.CoreList;
import space.cubicworld.core.database.nocache.CoreNoCacheDatabase;
import space.cubicworld.core.http.CoreHttpServer;
import space.cubicworld.core.list.CoreListContainer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

@Getter
public class CorePlugin {

    private final CoreDatabase database;
    private final CoreColorIndexContainer colorIndexContainer;
    private final CoreListContainer listContainer;
    private final CoreHttpServer httpServer;

    @Delegate
    private final CoreBootstrap bootstrap;

    public CorePlugin(CoreBootstrap bootstrap) {
        this.bootstrap = bootstrap;
        httpServer = new CoreHttpServer(this);
        database = new CoreNoCacheDatabase(this);
        listContainer = new CoreListContainer(this);
        colorIndexContainer = new CoreColorIndexContainer(this);
        httpServer.bind();
    }

    @SneakyThrows
    public void close() {
        database.close();
        httpServer.close();
    }

}
