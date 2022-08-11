package space.cubicworld.core.event;

import lombok.Builder;
import lombok.Data;
import space.cubicworld.core.model.CoreTeam;

@Data
@Builder
public class TeamDeleteEvent {

    private final CoreTeam team;

}
