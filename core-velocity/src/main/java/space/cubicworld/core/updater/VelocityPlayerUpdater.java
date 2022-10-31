package space.cubicworld.core.updater;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.CoreStatic;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.event.ColorChangeEvent;
import space.cubicworld.core.event.ReputationChangeEvent;
import space.cubicworld.core.event.TeamSelectEvent;
import space.cubicworld.core.json.CoreJsonObjectMapper;
import space.cubicworld.core.json.CoreLightPlayer;
import space.cubicworld.core.json.CoreLightPlayerImpl;

@RequiredArgsConstructor
public class VelocityPlayerUpdater {

    public static final ChannelIdentifier CHANNEL =
            MinecraftChannelIdentifier.from(CoreStatic.PLAYER_UPDATE_CHANNEL);

    private final VelocityPlugin plugin;

    public void update(CoreLightPlayer player) {
        plugin.getServer().getPlayer(player.getId())
                .flatMap(Player::getCurrentServer)
                .ifPresent(serverConnection -> serverConnection.sendPluginMessage(
                        CHANNEL, CoreJsonObjectMapper.writeBytes(CoreLightPlayerImpl.toImpl(player))
                ));
    }

    public void update(CorePlayer player) {
        player.asLight()
                .doOnNext(this::update)
                .subscribe();
    }

    @Subscribe
    public void colorChange(ColorChangeEvent event) {
        update(event.getPlayer());
    }

    @Subscribe
    public void selectedTeamChange(TeamSelectEvent event) {
        update(event.getPlayer());
    }

    @Subscribe
    public void serverChange(ServerPostConnectEvent event) {
        plugin.getDatabase()
                .fetchPlayer(event.getPlayer().getUniqueId())
                .doOnNext(this::update)
                .subscribe();
    }

    @Subscribe
    public void message(PluginMessageEvent event) {
        if (event.getIdentifier().equals(CHANNEL)) {
            event.setResult(PluginMessageEvent.ForwardResult.handled());
        }
    }

    @Subscribe
    public void reputationChange(ReputationChangeEvent event) {
        update(event.getPlayer());
    }

}
