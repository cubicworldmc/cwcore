package space.cubicworld.core.event;

import lombok.Data;
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.database.CoreTeam;

@Data
public class TeamLeaveEvent {

    private final CorePlayer player;
    private final CoreTeam team;

}
