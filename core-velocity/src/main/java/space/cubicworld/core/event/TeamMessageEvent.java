package space.cubicworld.core.event;

import lombok.Data;
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.database.CoreTeam;

@Data
public class TeamMessageEvent {

    private final CorePlayer sender;
    private final CoreTeam team;
    private final String message;

}
