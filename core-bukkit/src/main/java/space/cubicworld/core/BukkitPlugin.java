package space.cubicworld.core;

import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import space.cubicworld.core.cache.CoreCache;
import space.cubicworld.core.cache.CoreCacheSecondaryKey;
import space.cubicworld.core.database.CoreDatabase;
import space.cubicworld.core.listener.BukkitPlayerLoader;
import space.cubicworld.core.model.CorePlayer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class BukkitPlugin extends JavaPlugin {

    private static class CoreBukkitPlugin implements CorePlugin {

        private final BukkitPlugin plugin;
        @Getter
        private final Logger logger;

        public CoreBukkitPlugin(BukkitPlugin plugin) {
            this.plugin = plugin;
            logger = plugin.getSLF4JLogger();
        }

        @Override
        public InputStream readResource(String resource) {
            return plugin.getResource(resource);
        }

        @Override
        public Path getDataPath() {
            return plugin.getDataFolder().toPath();
        }
    }

    private final CorePlugin corePlugin = new CoreBukkitPlugin(this);
    private final Map<String, CorePlayer> players = new ConcurrentHashMap<>();
    private CoreDatabase database;

    @Override
    @SneakyThrows
    public void onEnable() {
        saveDefaultConfig();
        database = new CoreDatabase(
                getConfig().getString("mysql.host"),
                getConfig().getString("mysql.username"),
                getConfig().getString("mysql.password"),
                getConfig().getString("mysql.database"),
                corePlugin
        );
        getServer().getPluginManager().registerEvents(new BukkitPlayerLoader(this), this);
    }
}