package space.cubicworld.core.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import space.cubicworld.core.VelocityPlugin;

@RequiredArgsConstructor
public class VelocityJoinListener {

    private final VelocityPlugin plugin;

    @Subscribe
    public void join(PlayerChooseInitialServerEvent event) {
        Player player = event.getPlayer();
        plugin.getDatabase()
                .fetchPlayer(player.getUniqueId())
                .switchIfEmpty(
                        plugin.getDatabase().newPlayer(
                                player.getUniqueId(),
                                player.getUsername()
                        ).then(Mono.empty())
                )
                .flatMap(corePlayer -> {
                    if (!corePlayer.getName().equalsIgnoreCase(player.getUsername())) {
                        corePlayer.setName(player.getUsername());
                        return plugin.getDatabase().update(corePlayer);
                    }
                    return Mono.empty();
                })
                .subscribe();
    }

}
