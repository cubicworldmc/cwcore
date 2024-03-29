package space.cubicworld.core.listener;

import com.velocitypowered.api.event.Subscribe;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.database.CorePTRelation;
import space.cubicworld.core.event.TeamMessageEvent;
import space.cubicworld.core.message.CoreMessage;

@RequiredArgsConstructor
public class TeamMessageSender {

    private final VelocityPlugin plugin;

    @Subscribe
    public void teamMessage(TeamMessageEvent event) {
        CoreMessage.teamMessage(
                        event.getTeam(),
                        event.getSender(),
                        event.getMessage()
                ).flatMapMany(message -> event.getTeam()
                        .getAllRelations(CorePTRelation.Value.MEMBERSHIP)
                        .doOnNext(corePlayer -> plugin.getServer()
                                .getPlayer(corePlayer.getId())
                                .filter(plugin::isRealJoined)
                                .ifPresent(player -> player.sendMessage(message))
                        )
                )
                .subscribe();
    }

}
