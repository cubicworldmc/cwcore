package space.cubicworld.core.event;

import com.velocitypowered.api.proxy.Player;
import lombok.Builder;
import lombok.Data;
import space.cubicworld.core.model.CoreTeam;

@Data
@Builder
public class TeamInviteAcceptEvent {

    private final Player invited;
    private final CoreTeam team;

}
