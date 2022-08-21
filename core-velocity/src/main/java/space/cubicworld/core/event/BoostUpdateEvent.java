package space.cubicworld.core.event;

import lombok.Data;
import space.cubicworld.core.database.CoreBoost;

@Data
public class BoostUpdateEvent {

    private final CoreBoost boost;

}
