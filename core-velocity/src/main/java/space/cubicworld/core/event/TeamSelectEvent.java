package space.cubicworld.core.event;

import lombok.Data;
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.database.CoreTeam;

@Data
public class TeamSelectEvent {

    private final CorePlayer player;
    private final int previous;
    private final CoreTeam current;

}
