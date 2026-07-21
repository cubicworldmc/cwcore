package space.cubicworld.core.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PlayerUtils {

    public boolean isUuid(String id) {
        return id.length() == 36;
    }

}
