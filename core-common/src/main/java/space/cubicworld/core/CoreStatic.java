package space.cubicworld.core;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CoreStatic {

    public final String CWCORE_KEY = "cwcore";
    public final String PLAYER_UPDATE_CHANNEL = "player_update";

    @Getter @Setter
    private org.slf4j.Logger logger;

}
