package space.cubicworld.core.util;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.regex.Pattern;

@UtilityClass
public class ColorUtils {

    private final Pattern pattern = Pattern.compile("#[0-9a-fA-F]{3,6}");

    public TextColor checkedFromLocalized(String str) {
        NamedTextColor namedTextColor = NamedTextColor.NAMES.value(str);
        if (namedTextColor != null) return namedTextColor;
        if (str.length() != 4 && str.length() != 7) return null;
        return pattern.asPredicate().test(str) ? TextColor.fromCSSHexString(str) : null;
    }

    public TextColor fromLocalized(String str) {
        NamedTextColor namedTextColor = NamedTextColor.NAMES.value(str);
        if (namedTextColor != null) return namedTextColor;
        return TextColor.fromCSSHexString(str);
    }

}
