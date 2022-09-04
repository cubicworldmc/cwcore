package space.cubicworld.core.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.message.CoreMessage;

import java.util.Optional;

@RequiredArgsConstructor
public class VelocityJoinQuitMessagesListener {

    private final VelocityPlugin plugin;

    public void change(Player changer, RegisteredServer previous, RegisteredServer next) {
        plugin.getDatabase()
                .fetchPlayer(changer.getUniqueId())
                .ifPresent(corePlayer -> {
                    Component joinMessage = CoreMessage.joinMessage(corePlayer);
                    Component quitMessage = CoreMessage.quitMessage(corePlayer);
                    Optional.ofNullable(previous).map(RegisteredServer::getPlayersConnected)
                            .ifPresent(players -> players.stream()
                                    .filter(player -> !player.getUniqueId().equals(corePlayer.getId()))
                                    .forEach(player -> VelocityCoreCommandSource.sendSmallLocaleMessage(
                                            player, quitMessage
                                    ))
                            );
                    Optional.ofNullable(next).map(RegisteredServer::getPlayersConnected)
                            .ifPresent(players -> players.stream()
                                    .filter(player -> !player.getUniqueId().equals(corePlayer.getId()))
                                    .forEach(player -> VelocityCoreCommandSource.sendSmallLocaleMessage(
                                            player, joinMessage
                                    ))
                            );
                });
    }

    @Subscribe
    public void change(ServerConnectedEvent event) {
        change(
                event.getPlayer(),
                event.getPreviousServer().orElse(null),
                event.getServer()
        );
    }

    @Subscribe
    public void disconnect(DisconnectEvent event) {
        change(
                event.getPlayer(),
                event.getPlayer()
                        .getCurrentServer()
                        .map(ServerConnection::getServer)
                        .orElse(null),
                null
        );
    }

}
