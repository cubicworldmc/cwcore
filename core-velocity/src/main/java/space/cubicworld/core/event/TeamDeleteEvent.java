package space.cubicworld.core.event;

import lombok.Builder;
import lombok.Data;
import space.cubicworld.core.database.CoreTeam;

@Data
public class TeamDeleteEvent {

    private final CoreTeam team;

}
