package space.cubicworld.core;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BukkitPlayerLoader implements Listener {

    private static class LoadingTask {

        private boolean cancelled;

        public void setCancelled(boolean cancelled) {
            synchronized (this) {
                this.cancelled = cancelled;
            }
        }

        public boolean isCancelled() {
            return this.cancelled;
        }

    }

    private final Map<UUID, LoadingTask> tasks = new ConcurrentHashMap<>();

    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        BukkitPlugin plugin = BukkitPlugin.getInstance();
        LoadingTask task = new LoadingTask();
        UUID playerUuid = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(
                plugin, () -> {
                    try {
                        if (task.isCancelled()) return;
                        CorePlayer loaded = plugin
                                .getPlayerRepository()
                                .findPlayer(playerUuid)
                                .orElseGet(() -> CorePlayer
                                        .builder()
                                        .uuid(playerUuid)
                                        .build()
                                );
                        plugin.getOnlinePlayers()
                                .put(playerUuid, loaded);
                        tasks.remove(playerUuid);
                        if (task.isCancelled()) {
                            plugin.getOnlinePlayers().remove(playerUuid);
                        }
                    } catch (SQLException e) {
                        CoreStatic.getLogger().error("Failed to load user, kicking him:", e);
                        BukkitCoreUtils.internalPlayerKick(event.getPlayer());
                    }
                }
        );
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        LoadingTask task = tasks.get(playerUuid);
        if (task != null) {
            task.setCancelled(true);
        }
        BukkitPlugin.getInstance().getOnlinePlayers().remove(playerUuid);
    }

}
