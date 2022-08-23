package space.cubicworld.core.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import space.cubicworld.core.BukkitPlugin;
import space.cubicworld.core.json.CoreLightPlayerImpl;
import space.cubicworld.core.message.CoreMessage;

import java.util.Set;

@RequiredArgsConstructor
public class BukkitChatListener implements Listener {

    private final BukkitPlugin plugin;

    @EventHandler
    public void chat(AsyncChatEvent event) {
        boolean global = false;
        Component message = event.message();
        if (message instanceof TextComponent textComponent) {
            String content = textComponent.content();
            global = content.startsWith("!");
            if (global) {
                message = textComponent.toBuilder()
                        .content(content.substring(1))
                        .build();
            }
        }
        event.message(message);
        Player sender = event.getPlayer();
        if (!global) {
            Set<Audience> viewers = event.viewers();
            try {
                int localRadius = plugin.getConfig().getInt("chat.local-radius");
                viewers.removeIf(viewer -> viewer instanceof Player viewerPlayer && !(
                                viewerPlayer.getWorld().equals(sender.getWorld()) &&
                                        Math.abs(viewerPlayer.getLocation().getX() - sender.getLocation().getX()) <= localRadius &&
                                        Math.abs(viewerPlayer.getLocation().getZ() - sender.getLocation().getZ()) <= localRadius
                        )
                );
            } catch (UnsupportedOperationException e) {
                plugin.getLogger().warning("AsyncChatEvent has immutable set of viewers can not edit them, just cancelling");
                event.setCancelled(true);
            }
        }
        Component endMessage = CoreMessage.message(
                plugin.getCorePlayers().computeIfAbsent(
                        sender.getUniqueId(),
                        id -> CoreLightPlayerImpl.defaultImpl(id, sender.getName())
                ),
                message
        );
        if (!global) {
            endMessage = Component.empty()
                    .append(Component.text("L").color(NamedTextColor.GRAY))
                    .append(Component.space())
                    .append(endMessage);
        }
        final Component finalEndMessage = endMessage;
        event.renderer((source, sourceDisplayName, message1, viewer) -> finalEndMessage);
    }

}
