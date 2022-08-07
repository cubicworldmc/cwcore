package space.cubicworld.core;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Cleanup;
import lombok.Getter;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import space.cubicworld.core.cache.CoreSecondaryCache;
import space.cubicworld.core.command.VelocityCommandHelper;
import space.cubicworld.core.command.VelocityCoreCommand;
import space.cubicworld.core.command.admin.AdminCommand;
import space.cubicworld.core.command.reputation.ReputationCommand;
import space.cubicworld.core.command.team.TeamCommand;
import space.cubicworld.core.listener.VelocityJoinListener;
import space.cubicworld.core.message.CoreMessage;
import space.cubicworld.core.model.CorePlayer;
import space.cubicworld.core.model.CoreTeam;
import space.cubicworld.core.notification.TeamInvitationNotification;

import java.io.*;
import java.nio.file.Path;
import java.util.UUID;

@Plugin(
        id = "cwcore",
        name = "CWCore",
        authors = "Jenya705",
        description = "Core plugin for cubic"
)
@Getter
public class VelocityPlugin {

    private final ProxyServer server;
    private final CorePlugin core;
    private final FileConfig config;
    private final Logger logger;

    private final CoreSecondaryCache<String, UUID, CorePlayer> playerByName;
    private final CoreSecondaryCache<String, Integer, CoreTeam> teamByName;

    @Inject
    public VelocityPlugin(
            ProxyServer server,
            @DataDirectory Path dataDirectory,
            Logger logger
    ) throws ClassNotFoundException, IOException {
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
        this.core = new CorePlugin(
                config.get("mysql.host"),
                config.get("mysql.username"),
                config.get("mysql.password"),
                config.get("mysql.database")
        );
        playerByName = new CoreSecondaryCache<>(
                core,
                CorePlayer.class,
                "FROM CorePlayer p WHERE p.name = :key",
                CorePlayer::getUuid
        );
        teamByName = new CoreSecondaryCache<>(
                core,
                CoreTeam.class,
                "FROM CoreTeam t WHERE t.name = :key",
                CoreTeam::getId
        );
    }

    @Subscribe
    public void initialize(ProxyInitializeEvent event) {
        new VelocityCoreCommand(new ReputationCommand(this)).register(this);
        new VelocityCoreCommand(new AdminCommand(this)).register(this);
        new VelocityCoreCommand(new TeamCommand(this)).register(this);
        server.getEventManager().register(this, new TeamInvitationNotification(this));
        server.getEventManager().register(this, new VelocityJoinListener(this));
    }

    @Subscribe
    public void shutdown(ProxyShutdownEvent event) {
        core.getHibernateSessionFactory().close();
    }

    /**
     * Ensures that transaction is active
     *
     * @return Already active transaction
     */
    public Transaction beginTransaction() {
        return core.beginTransaction();
    }

    /**
     * Ensures that transaction is committed
     *
     * @return Already committed transaction
     */
    public Transaction commitTransaction() {
        return core.commitTransaction();
    }

    /**
     * Macros for: getCore().getHibernateSessionFactory().getCurrentSession()
     *
     * @return Current hibernate session
     */
    public Session currentSession() {
        return core
                .getHibernateSessionFactory()
                .getCurrentSession();
    }

    public VelocityCommandHelper commandHelper() {
        return new VelocityCommandHelper(this);
    }

}
