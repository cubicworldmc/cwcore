package space.cubicworld.core.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;

@RequiredArgsConstructor
public class VelocityJoinListener {

    private final VelocityPlugin plugin;

    @Subscribe
    public void join(PlayerChooseInitialServerEvent event) {
        if (plugin.getDatabase()
                .fetchPlayer(event.getPlayer().getUniqueId())
                .isEmpty()
        ) {
            plugin.getDatabase()
                    .newPlayer(
                            event.getPlayer().getUniqueId(),
                            event.getPlayer().getUsername()
                    );
        }
    }

}
