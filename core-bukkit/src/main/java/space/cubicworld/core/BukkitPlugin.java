package space.cubicworld.core;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import space.cubicworld.core.database.CoreDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

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
    private CoreDatabase database;

    @Override
    public void onEnable() {

    }
}