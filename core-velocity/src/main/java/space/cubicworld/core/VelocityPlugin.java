package space.cubicworld.core;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.SettingsManagerBuilder;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import org.slf4j.Logger;
import space.cubicworld.core.cache.CoreCache;
import space.cubicworld.core.cache.CoreCacheSecondaryKey;
import space.cubicworld.core.cache.ReferencedCoreCache;
import space.cubicworld.core.command.CWCoreCommand;
import space.cubicworld.core.command.ReputationCommand;
import space.cubicworld.core.command.TeamCommand;
import space.cubicworld.core.database.CoreDatabase;
import space.cubicworld.core.listener.PlayerLoader;
import space.cubicworld.core.message.CoreMessageContainer;
import space.cubicworld.core.model.CorePlayer;
import space.cubicworld.core.model.CoreTeam;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Plugin(
        name = "CWCore",
        id = "cwcore",
        authors = "Jenya705",
        version = "1.0"
)
@Getter
public class VelocityPlugin implements CorePlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataPath;
    private final SettingsManager config;
    private final CoreDatabase database;
    private final CoreMessageContainer messageContainer;
    private final VelocityPlayerUpdater playerUpdater;
    private final VelocityCache cache = new VelocityCache(this);

    @Inject
    public VelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataPath)
            throws IOException, SQLException, ClassNotFoundException {
        this.server = server;
        this.logger = logger;
        this.dataPath = dataPath;
        config = SettingsManagerBuilder
                .withYamlFile(dataPath.resolve("config.yml"))
                .configurationData(VelocityConfig.class)
                .useDefaultMigrationService()
                .create();
        config.save();
        messageContainer = new CoreMessageContainer(this);
        playerUpdater = new VelocityPlayerUpdater(this);
        database = new CoreDatabase(
                config.getProperty(VelocityConfig.SQL_HOST),
                config.getProperty(VelocityConfig.SQL_USERNAME),
                config.getProperty(VelocityConfig.SQL_PASSWORD),
                config.getProperty(VelocityConfig.SQL_DATABASE),
                this
        );
    }

    @Subscribe
    public void initialization(ProxyInitializeEvent event) {
        server.getEventManager().register(this, new PlayerLoader(this));
        server.getEventManager().register(this, playerUpdater);
        server.getChannelRegistrar().register(VelocityPlayerUpdater.IDENTIFIER);
        server.getCommandManager().register("cwcore", new CWCoreCommand(this));
        server.getCommandManager().register("reputation", new ReputationCommand(this), "rep");
        server.getCommandManager().register("team", new TeamCommand(this));
    }

    @Subscribe
    public void stopping(ProxyShutdownEvent event) {
        /* NOTHING */
    }

    @Override
    public InputStream readResource(String resource) {
        return getClass().getClassLoader().getResourceAsStream(resource);
    }

}
