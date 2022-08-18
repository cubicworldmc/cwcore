package space.cubicworld.core.event;

import lombok.Data;
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.database.CoreTeam;

@Data
public class TeamKickEvent {

    private final CorePlayer kicked;
    private final CoreTeam team;

}
