package space.cubicworld.core;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import space.cubicworld.core.json.CoreLightPlayer;
import space.cubicworld.core.json.CoreLightPlayerImpl;
import space.cubicworld.core.json.CoreJsonObjectMapper;

@RequiredArgsConstructor
public class BukkitPlayerUpdater implements Listener, PluginMessageListener {

    private final BukkitPlugin plugin;

    @Override
    @SneakyThrows
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        CoreLightPlayer corePlayer = CoreJsonObjectMapper.readBytes(message, CoreLightPlayerImpl.class);
        plugin.getCorePlayers().put(player.getUniqueId(), corePlayer);
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
        plugin.getCorePlayers().remove(event.getPlayer().getUniqueId());
    }

}
