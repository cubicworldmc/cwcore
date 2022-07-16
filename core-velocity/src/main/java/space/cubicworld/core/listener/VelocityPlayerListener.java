package space.cubicworld.core.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.CoreStatic;
import space.cubicworld.core.VelocityPlugin;

import java.sql.SQLException;

@RequiredArgsConstructor
public class VelocityPlayerListener {

    private final VelocityPlugin plugin;

    @Subscribe
    public void playerJoin(PlayerChooseInitialServerEvent event) {
        try {
            plugin.getPlayerRepository()
                    .insertDefault(event.getPlayer().getUniqueId(), event.getPlayer().getUsername());
        } catch (SQLException e) {
            CoreStatic.getLogger()
                    .error("Error while trying to insert default user for {}:", event.getPlayer(), e);
        }
    }

}
