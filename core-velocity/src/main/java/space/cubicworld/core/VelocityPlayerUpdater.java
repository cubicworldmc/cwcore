package space.cubicworld.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.model.CorePlayer;

import java.sql.SQLException;
import java.util.Optional;

@RequiredArgsConstructor
public class VelocityPlayerUpdater {

    public static final ChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier
            .create(CoreStatic.CWCORE_KEY, CoreStatic.UPDATE_PLAYER);

    private final VelocityPlugin plugin;

    public void update(CorePlayer corePlayer) throws SQLException, JsonProcessingException {
        Optional<ServerConnection> connection = plugin.getServer()
                .getPlayer(corePlayer.getUuid())
                .flatMap(Player::getCurrentServer);
        if (connection.isPresent()) {
            connection.get().sendPluginMessage(
                    IDENTIFIER,
                    CoreStatic.MAPPER.writeValueAsBytes(corePlayer)
            );
        }
        CorePlayer.updateStatement().update(plugin.getDatabase(), corePlayer);
    }

}
