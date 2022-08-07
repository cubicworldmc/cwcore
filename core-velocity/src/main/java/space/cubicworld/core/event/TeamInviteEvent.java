package space.cubicworld.core.event;

import com.velocitypowered.api.proxy.Player;
import lombok.Builder;
import lombok.Data;
import space.cubicworld.core.model.CorePlayer;
import space.cubicworld.core.model.CoreTeam;

@Data
@Builder
public class TeamInviteEvent {

    private final Player inviter;
    private final CorePlayer invited;
    private final CoreTeam team;

}
