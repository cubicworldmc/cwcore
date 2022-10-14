package space.cubicworld.core.listener;

import com.velocitypowered.api.event.Subscribe;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.database.CorePTRelation;
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.event.RealJoinEvent;
import space.cubicworld.core.event.TeamInviteEvent;
import space.cubicworld.core.message.CoreMessage;

@RequiredArgsConstructor
public class TeamInvitationNotification {

    private final VelocityPlugin plugin;

    @Subscribe
    public void invite(TeamInviteEvent event) {
        plugin.getServer()
                .getPlayer(event.getInvited().getId())
                .filter(plugin::isRealJoined)
                .ifPresent(player -> plugin
                        .getDatabase()
                        .fetchPlayer(event.getInviter().getUniqueId())
                        .flatMap(inviter -> CoreMessage.teamInvite(inviter, event.getTeam()))
                        .doOnNext(teamInviteComponent -> VelocityCoreCommandSource.sendLocaleMessage(player, teamInviteComponent))
                        .subscribe()
                );
    }

    @Subscribe
    public void join(RealJoinEvent event) {
        plugin
                .getDatabase()
                .fetchPlayer(event.getPlayer().getUniqueId())
                .flatMap(corePlayer -> corePlayer.getRelationsCount(CorePTRelation.Value.INVITE)
                        .filter(invitesCount -> invitesCount != 0)
                        .map(CoreMessage::teamInviteJoinNotification)
                )
                .doOnNext(message -> VelocityCoreCommandSource.sendLocaleMessage(event.getPlayer(), message))
                .subscribe();
    }

}
