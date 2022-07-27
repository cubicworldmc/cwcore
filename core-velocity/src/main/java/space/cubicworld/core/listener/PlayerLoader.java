package space.cubicworld.core.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.model.CorePlayer;
import space.cubicworld.core.model.CoreStatement;

import java.sql.SQLException;

@RequiredArgsConstructor
public class PlayerLoader {

    private final VelocityPlugin plugin;

    @Subscribe
    public void join(PlayerChooseInitialServerEvent event) throws SQLException {
        CorePlayer.insertStatement().update(
                plugin.getDatabase(),
                CorePlayer
                        .builder()
                        .uuid(event.getPlayer().getUniqueId())
                        .name(event.getPlayer().getUsername())
                        .build()
        );
        plugin.getCache().loadPlayer(event.getPlayer().getUniqueId(), false);
    }

    @Subscribe
    public void quit(DisconnectEvent event) {
        plugin.getCache().removePlayer(event.getPlayer().getUniqueId());
    }

}
