package space.cubicworld.core;

import com.electronwill.nightconfig.core.AbstractConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Cleanup;
import lombok.Getter;
import org.slf4j.Logger;
import space.cubicworld.core.command.VelocityCommandHelper;
import space.cubicworld.core.command.VelocityCoreCommand;
import space.cubicworld.core.command.admin.AdminCommand;
import space.cubicworld.core.command.boost.BoostCommand;
import space.cubicworld.core.command.color.ColorCommand;
import space.cubicworld.core.command.profile.ProfileCommand;
import space.cubicworld.core.command.reputation.ReputationCommand;
import space.cubicworld.core.command.team.TeamCommand;
import space.cubicworld.core.command.team.TeamMessageAliasCommand;
import space.cubicworld.core.command.top.TopCommand;
import space.cubicworld.core.database.CoreDatabase;
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.listener.*;
import space.cubicworld.core.message.CoreMessage;
import space.cubicworld.core.scheduler.BoostPremiumScheduler;
import space.cubicworld.core.updater.VelocityPlayerUpdater;

import java.io.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;

@Plugin(
        id = "cwcore",
        name = "CWCore",
        authors = "Jenya705",
        description = "Core plugin for cubic",
        dependencies = @Dependency(id = "luckperms")
)
@Getter
public class VelocityPlugin {

    private final ProxyServer server;
    private final CorePlugin core;
    private final FileConfig config;
    private final Logger logger;

    private final Set<UUID> realJoined = Collections.synchronizedSet(new HashSet<>());

    @Inject
    public VelocityPlugin(
            ProxyServer server,
            @DataDirectory Path dataDirectory,
            Logger logger
    ) throws ClassNotFoundException, IOException, SQLException {
        Class.forName("com.electronwill.nightconfig.yaml.YamlFormat");
        this.server = server;
        this.logger = logger;
        CoreMessage.register(getClass().getClassLoader(), logger);
        dataDirectory.toFile().mkdirs();
        File configFile = dataDirectory.resolve("config.yml").toFile();
        if (!configFile.exists()) {
            configFile.createNewFile();
            @Cleanup InputStream resourcesConfigIs = getClass().getClassLoader().getResourceAsStream("config.yml");
            if (resourcesConfigIs != null) {
                @Cleanup OutputStream configOs = new FileOutputStream(configFile);
                byte[] buffer = new byte[1024];
                int length;
                while (true) {
                    length = resourcesConfigIs.read(buffer);
                    if (length == 0) break;
                    configOs.write(buffer, 0, length);
                }
                configOs.flush();
            } else {
                logger.warn("Could not find config.yml in plugin resources");
            }
        }
        config = FileConfig
                .builder(configFile)
                .concurrent()
                .autosave()
                .build();
        config.load();
        AbstractConfig colorsConfig = config.get("colors");
        Map<String, String> colors = new LinkedHashMap<>();
        colorsConfig.valueMap().forEach((key, value) -> colors.put(key, value.toString()));
        this.core = new CorePlugin(
                config.get("mysql.host"),
                config.get("mysql.username"),
                config.get("mysql.password"),
                config.get("mysql.database"),
                getClass().getClassLoader(),
                new VelocityCoreResolver(this),
                colors
        );
    }

    @Subscribe
    public void initialize(ProxyInitializeEvent event) {
        new VelocityCoreCommand(new ReputationCommand(this)).register(this);
        new VelocityCoreCommand(new AdminCommand(this)).register(this);
        new VelocityCoreCommand(new TeamCommand(this)).register(this);
        new VelocityCoreCommand(new ColorCommand(this)).register(this);
        new VelocityCoreCommand(new BoostCommand(this)).register(this);
        new VelocityCoreCommand(new ProfileCommand(this)).register(this);
        new VelocityCoreCommand(new TeamMessageAliasCommand(this)).register(this);
        new VelocityCoreCommand(new TopCommand(this)).register(this);
        server.getEventManager().register(this, new TeamInvitationNotification(this));
        server.getEventManager().register(this, new VelocityJoinListener(this));
        server.getEventManager().register(this, new VelocityRealJoin(this));
        server.getEventManager().register(this, new TeamMessageSender(this));
        server.getEventManager().register(this, new BoostPremiumScheduler(this));
        server.getEventManager().register(this, new VelocityPlayerUpdater(this));
        server.getEventManager().register(this, new VelocityJoinQuitMessagesListener(this));
    }

    @Subscribe
    public void shutdown(ProxyShutdownEvent event) throws Exception {

    }

    public VelocityCommandHelper commandHelper() {
        return new VelocityCommandHelper(this);
    }

    public CoreDatabase getDatabase() {
        return core.getDatabase();
    }

    public boolean isRealJoined(Player player) {
        return realJoined.contains(player.getUniqueId());
    }

    public boolean isRealJoined(CorePlayer player) {
        return realJoined.contains(player.getId());
    }

}
