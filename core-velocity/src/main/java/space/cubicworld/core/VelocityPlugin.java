package space.cubicworld.core;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.SettingsManagerBuilder;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import org.slf4j.Logger;
import space.cubicworld.core.command.ColorCommand;
import space.cubicworld.core.command.WorldColorCommand;
import space.cubicworld.core.database.DatabaseModule;
import space.cubicworld.core.listener.VelocityPlayerListener;
import space.cubicworld.core.message.CoreMessageContainer;
import space.cubicworld.core.model.CoreModelCaching;
import space.cubicworld.core.model.CorePlayer;
import space.cubicworld.core.model.CoreTeam;
import space.cubicworld.core.repository.CorePlayerRepository;
import space.cubicworld.core.repository.CoreTeamRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.UUID;

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
    private final CoreTeamRepository teamRepository;
    private final CoreMessageContainer messageContainer;
    private final VelocityCoreUtils utils = new VelocityCoreUtils(this);
    private final CoreModelCaching<String, CorePlayer> playersCache;
    private final CoreModelCaching<String, CoreTeam> teamsCache;

    @Inject
    public VelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory)
            throws SQLException, ClassNotFoundException, IOException {
        this.server = server;
        CoreStatic.setLogger(logger);
        messageContainer = new CoreMessageContainer(fileName -> getClass().getClassLoader().getResourceAsStream(fileName));
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
                getClass().getClassLoader().getResourceAsStream("hikari.properties")
        );
        playerRepository = new CorePlayerRepository(databaseModule);
        teamRepository = new CoreTeamRepository(databaseModule);
        playersCache = new CoreModelCaching<>(name -> {
            try {
                return playerRepository.findPlayer(name).orElse(null);
            } catch (SQLException e) {
                CoreStatic.getLogger().error("Failed to fetch player {}: ", name, e);
            }
            return null;
        });
        teamsCache = new CoreModelCaching<>(name -> {
            try {
                return teamRepository.findTeam(name).orElse(null);
            } catch (SQLException e) {
                CoreStatic.getLogger().error("Failed to fetch team {}:", name, e);
            }
            return null;
        });
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
