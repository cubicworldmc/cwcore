package space.cubicworld.core.event;

import lombok.Data;
import space.cubicworld.core.database.CorePlayer;

@Data
public class BoostAddedEvent {

    private final CorePlayer player;

}
