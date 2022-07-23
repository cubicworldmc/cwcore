package space.cubicworld.core;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

@UtilityClass
public class CoreConfigUtils {

    public TextColor fromString(String color) {
        NamedTextColor namedTextColor = NamedTextColor.NAMES.value(color);
        if (namedTextColor == null) return TextColor.fromCSSHexString(color);
        return namedTextColor;
    }

}
