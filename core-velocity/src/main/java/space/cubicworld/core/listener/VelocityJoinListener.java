package space.cubicworld.core.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;

@RequiredArgsConstructor
public class VelocityJoinListener {

    private final VelocityPlugin plugin;

    @Subscribe
    public void join(PlayerChooseInitialServerEvent event) {
        Player player = event.getPlayer();
        plugin.getDatabase()
                .fetchPlayer(player.getUniqueId())
                .ifPresentOrElse(
                        corePlayer -> {
                            if (!corePlayer.getName().equalsIgnoreCase(player.getUsername())) {
                                corePlayer.setName(player.getUsername());
                                plugin.getDatabase().update(corePlayer);
                            }
                        },
                        () -> plugin.getDatabase().newPlayer(
                                player.getUniqueId(),
                                player.getUsername()
                        )
                );
    }

}
