package space.cubicworld.core;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.SettingsManagerBuilder;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import org.slf4j.Logger;
import space.cubicworld.core.command.ColorCommand;
import space.cubicworld.core.command.WorldColorCommand;
import space.cubicworld.core.database.DatabaseModule;
import space.cubicworld.core.listener.VelocityPlayerListener;
import space.cubicworld.core.repository.CorePlayerRepository;

import java.nio.file.Path;
import java.sql.SQLException;

@Plugin(
        name = "CWCore",
        id = "cwcore",
        authors = "Jenya705",
        version = "1.0"
)
@Getter
public class VelocityPlugin {

    private final ProxyServer server;
    private final VelocityPlayerUpdater playerUpdater = new VelocityPlayerUpdater(this);
    private final SettingsManager config;
    private final DatabaseModule databaseModule;
    private final CorePlayerRepository playerRepository;

    @Inject
    public VelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory)
            throws SQLException, ClassNotFoundException {
        this.server = server;
        CoreStatic.setLogger(logger);
        config = SettingsManagerBuilder
                .withYamlFile(dataDirectory.resolve("config.yml"))
                .configurationData(VelocityConfig.class)
                .useDefaultMigrationService()
                .create();
        config.save();
        databaseModule = new DatabaseModule(
                config.getProperty(VelocityConfig.SQL_HOST),
                config.getProperty(VelocityConfig.SQL_USERNAME),
                config.getProperty(VelocityConfig.SQL_PASSWORD),
                config.getProperty(VelocityConfig.SQL_DATABASE),
                getClass().getResourceAsStream("hikari.properties")
        );
        playerRepository = new CorePlayerRepository(databaseModule);
    }

    @Subscribe
    public void initialize(ProxyInitializeEvent event) {
        server.getChannelRegistrar().register(VelocityPlayerUpdater.PLAYER_UPDATE_IDENTIFIER);
        server.getEventManager().register(this, playerUpdater);
        server.getEventManager().register(this, new VelocityPlayerListener(this));
        server.getCommandManager().register("color", new ColorCommand(this));
        server.getCommandManager().register("wcolor", new WorldColorCommand(this));
    }

}
