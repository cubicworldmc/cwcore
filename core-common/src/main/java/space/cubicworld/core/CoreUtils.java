package space.cubicworld.core;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.Locale;

@UtilityClass
public class CoreUtils {

    public TextColor getColorNamed(String color) {
        if (color == null) return NamedTextColor.WHITE;
        TextColor textColor = NamedTextColor.NAMES.value(color.toLowerCase(Locale.ROOT));
        if (textColor != null) return textColor;
        return TextColor.fromCSSHexString(color);
    }

}
