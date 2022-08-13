package space.cubicworld.core.event;

import com.velocitypowered.api.proxy.Player;
import lombok.Builder;
import lombok.Data;
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.database.CoreTeam;

@Data
@Builder
public class TeamInviteEvent {

    private final Player inviter;
    private final CorePlayer invited;
    private final CoreTeam team;

}
