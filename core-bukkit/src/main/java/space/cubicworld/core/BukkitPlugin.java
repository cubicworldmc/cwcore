package space.cubicworld.core;

import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import space.cubicworld.core.database.DatabaseModule;
import space.cubicworld.core.repository.CorePlayerRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class BukkitPlugin extends JavaPlugin {

    @Getter
    private static BukkitPlugin instance;

    private DatabaseModule databaseModule;
    private CorePlayerRepository playerRepository;
    private final Map<UUID, CorePlayer> onlinePlayers = new ConcurrentHashMap<>();

    @Override
    @SneakyThrows
    public void onEnable() {
        instance = this;
        CoreStatic.setLogger(getSLF4JLogger());
        saveDefaultConfig();
        databaseModule = new DatabaseModule(
                getConfig().getString("sql.host"),
                getConfig().getString("sql.username"),
                getConfig().getString("sql.password"),
                getConfig().getString("sql.database"),
                getResource("hikari.properties")
        );
        playerRepository = new CorePlayerRepository(databaseModule);
        getServer().getPluginManager().registerEvents(new BukkitPlayerLoader(), this);
        getServer().getMessenger().registerIncomingPluginChannel(
                this, BukkitPlayerUpdater.PLAYER_UPDATE_CHANNEL, new BukkitPlayerUpdater());
        new CorePapiExpansion().register();
    }

    @Override
    public void onDisable() {
        if (databaseModule != null) databaseModule.close();
    }

    public Optional<CorePlayer> getOptionalPlayer(UUID uuid) {
        return Optional.ofNullable(getOnlinePlayers().get(uuid));
    }

}
