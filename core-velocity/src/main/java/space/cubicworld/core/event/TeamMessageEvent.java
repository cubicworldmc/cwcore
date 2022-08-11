package space.cubicworld.core.event;

import com.velocitypowered.api.proxy.Player;
import lombok.Builder;
import lombok.Data;
import space.cubicworld.core.model.CoreTeam;

@Data
@Builder
public class TeamMessageEvent {

    private final Player sender;
    private final CoreTeam team;
    private final String message;

}
