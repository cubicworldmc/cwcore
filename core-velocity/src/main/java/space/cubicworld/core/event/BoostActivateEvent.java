package space.cubicworld.core.event;

import lombok.Data;
import space.cubicworld.core.database.CoreBoost;

@Data
public class BoostActivateEvent {

    private final CoreBoost boost;
    private final boolean extend;

}
