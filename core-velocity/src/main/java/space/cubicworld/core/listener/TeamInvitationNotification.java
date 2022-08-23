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
        plugin.getServer().getPlayer(event.getInvited().getId())
                .filter(plugin::isRealJoined)
                .ifPresent(player -> {
                    try {
                        VelocityCoreCommandSource.sendLocaleMessage(player,
                                CoreMessage.teamInvite(
                                        plugin
                                                .getDatabase()
                                                .fetchPlayer(event.getInviter().getUniqueId())
                                                .orElseThrow(),
                                        event.getTeam()
                                )
                        );
                    } catch (Exception e) {
                        plugin.getLogger().error("Failed to fetch player:", e);
                    }
                });
    }

    @Subscribe
    public void join(RealJoinEvent event) {
        CorePlayer corePlayer = plugin
                .getDatabase()
                .fetchPlayer(event.getPlayer().getUniqueId())
                .orElseThrow();
        int invitesCount = corePlayer.getRelationsCount(CorePTRelation.Value.INVITE);
        if (invitesCount == 0) return;
        VelocityCoreCommandSource.sendLocaleMessage(
                event.getPlayer(),
                CoreMessage.teamInviteJoinNotification(invitesCount)
        );
    }

}
