package space.cubicworld.core.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;

import java.sql.SQLException;

@RequiredArgsConstructor
public class VelocityJoinListener {

    private final VelocityPlugin plugin;

    @Subscribe
    public void join(PlayerChooseInitialServerEvent event) throws SQLException {
        if (!plugin.getDatabase()
                .fetchPlayerByUuid(event.getPlayer().getUniqueId())
                .isActuallyExists()
        ) {
            plugin.getDatabase()
                    .newPlayer(
                            event.getPlayer().getUniqueId(),
                            event.getPlayer().getUsername()
                    )
                    .update();
        }
    }

}
