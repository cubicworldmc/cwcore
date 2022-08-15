package space.cubicworld.core.event;

import com.velocitypowered.api.proxy.Player;
import lombok.Data;
import space.cubicworld.core.database.CoreTeam;

@Data
public class TeamCreateEvent {

    private final Player owner;
    private final CoreTeam team;

}
