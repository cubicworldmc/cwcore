package space.cubicworld.core;

import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.plugin.java.JavaPlugin;
import space.cubicworld.core.database.DatabaseModule;
import space.cubicworld.core.message.CoreMessageContainer;
import space.cubicworld.core.repository.CorePlayerRepository;

@Getter
public class BukkitPlugin extends JavaPlugin {

    @Getter
    private static BukkitPlugin instance;

    private DatabaseModule databaseModule;
    private CorePlayerRepository playerRepository;
    private BukkitPlayerLoader playerLoader;

    private CoreMessageContainer messageContainer;

    @Override
    @SneakyThrows
    public void onEnable() {
        instance = this;
        CoreStatic.setLogger(getSLF4JLogger());
        messageContainer = new CoreMessageContainer(this::getResource);
        saveDefaultConfig();
        databaseModule = new DatabaseModule(
                getConfig().getString("sql.host"),
                getConfig().getString("sql.username"),
                getConfig().getString("sql.password"),
                getConfig().getString("sql.database"),
                getResource("hikari.properties")
        );
        playerRepository = new CorePlayerRepository(databaseModule);
        playerLoader = new BukkitPlayerLoader();
        getServer().getPluginManager().registerEvents(playerLoader, this);
        getServer().getMessenger().registerIncomingPluginChannel(
                this, BukkitPlayerUpdater.PLAYER_UPDATE_CHANNEL, new BukkitPlayerUpdater());
        new CorePapiExpansion().register();
    }

    @Override
    public void onDisable() {
        if (databaseModule != null) databaseModule.close();
    }

}
