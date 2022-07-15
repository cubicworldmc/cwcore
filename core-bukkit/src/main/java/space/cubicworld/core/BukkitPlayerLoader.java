package space.cubicworld.core;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import space.cubicworld.core.model.CoreModelCaching;
import space.cubicworld.core.model.CorePlayer;

import java.sql.SQLException;
import java.util.UUID;

public class BukkitPlayerLoader extends CoreModelCaching<UUID, CorePlayer> implements Listener {

    public BukkitPlayerLoader() {
        super((playerUuid, consumer) ->
                Bukkit.getScheduler().runTaskAsynchronously(
                        BukkitPlugin.getInstance(),
                        () -> {
                            try {
                                consumer.accept(BukkitPlugin
                                        .getInstance()
                                        .getPlayerRepository()
                                        .findPlayer(playerUuid)
                                        .orElseGet(() -> CorePlayer
                                                .builder()
                                                .uuid(playerUuid)
                                                .build()
                                        )
                                );
                            } catch (SQLException e) {
                                CoreStatic.getLogger().error("Failed to load user, kicking him:", e);
                                BukkitCoreUtils.internalPlayerKick(Bukkit.getPlayer(playerUuid));
                                consumer.accept(null);
                            }
                        }
                )
        );
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        loadPermanent(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
        unload(event.getPlayer().getUniqueId());
    }

}
