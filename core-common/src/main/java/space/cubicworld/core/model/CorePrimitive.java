package space.cubicworld.core.model;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.format.TextColor;

@UtilityClass
public class CorePrimitive {

    public Integer toSQL(TextColor color) {
        return color == null ? null : color.value();
    }

    public TextColor toColor(Integer value) {
        return value == null ? null : TextColor.color(value);
    }

}
