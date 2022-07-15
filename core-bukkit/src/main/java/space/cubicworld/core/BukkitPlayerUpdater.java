package space.cubicworld.core;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import space.cubicworld.core.parser.CoreSerializer;
import space.cubicworld.core.util.Pair;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class BukkitPlayerUpdater implements PluginMessageListener {

    public static final String PLAYER_UPDATE_CHANNEL = "%s:%s"
            .formatted(CoreStatic.CWCORE_KEY, CoreStatic.PLAYER_UPDATE_CHANNEL);

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        if (!channel.equals(PLAYER_UPDATE_CHANNEL)) return;
        CorePlayer corePlayer = BukkitPlugin
                .getInstance()
                .getOptionalPlayer(player.getUniqueId())
                .orElse(null);
        if (corePlayer == null) return;
        try {
            String messageString = new String(message, StandardCharsets.UTF_8);
            CoreSerializer.readInto(corePlayer, messageString);
        } catch (Throwable e) {
            CoreStatic.getLogger()
                    .error("Received bad message for player_update:", e);
        }
    }
}
