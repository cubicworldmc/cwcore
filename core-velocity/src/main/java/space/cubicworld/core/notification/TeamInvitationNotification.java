package space.cubicworld.core.notification;

import com.velocitypowered.api.event.Subscribe;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.event.TeamInviteEvent;

@RequiredArgsConstructor
public class TeamInvitationNotification {

    private final VelocityPlugin plugin;

    @Subscribe
    public void invite(TeamInviteEvent event) {
        plugin.getServer().getPlayer(event.getInvited().getUuid())
                .ifPresent(player -> {});
    }

}
