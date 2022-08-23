package space.cubicworld.core.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.event.RealJoinEvent;

import java.util.*;

@RequiredArgsConstructor
public class VelocityRealJoin {

    private final VelocityPlugin plugin;

    @Subscribe
    public void changeServer(ServerConnectedEvent event) {
        if (plugin.getRealJoined().contains(event.getPlayer().getUniqueId())) return;
        List<String> ignoredServers = plugin.getConfig().get("ignored-servers");
        if (ignoredServers != null && ignoredServers.contains(event.getServer().getServerInfo().getName())) return;
        plugin.getRealJoined().add(event.getPlayer().getUniqueId());
        plugin.getServer().getEventManager().fireAndForget(
                new RealJoinEvent(event.getPlayer(), event.getServer())
        );
    }

    @Subscribe
    public void disconnect(DisconnectEvent event) {
        plugin.getRealJoined().remove(event.getPlayer().getUniqueId());
    }

}
