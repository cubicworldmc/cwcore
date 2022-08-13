package space.cubicworld.core.listener;

import com.velocitypowered.api.event.Subscribe;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.event.TeamInviteEvent;
import space.cubicworld.core.message.CoreMessage;

@RequiredArgsConstructor
public class TeamInvitationNotification {

    private final VelocityPlugin plugin;

    @Subscribe
    public void invite(TeamInviteEvent event) {
        plugin.getServer().getPlayer(event.getInvited().getUuid())
                .ifPresent(player -> {
                    try {
                        VelocityCoreCommandSource.sendLocaleMessage(player,
                                CoreMessage.teamInvite(
                                        plugin
                                                .getDatabase()
                                                .fetchPlayerByUuid(event.getInviter().getUniqueId()),
                                        event.getTeam()
                                )
                        );
                    } catch (Exception e) {
                        plugin.getLogger().error("Failed to fetch player:", e);
                    }
                });
    }

}
