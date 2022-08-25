package space.cubicworld.core.event;

import lombok.Data;
import space.cubicworld.core.database.CorePlayer;

@Data
public class ReputationChangeEvent {

    private final CorePlayer player;
    private final int previous;
    private final int current;

}
