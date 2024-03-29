package space.cubicworld.core.color;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Nullable;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CoreColor {

    private static final CoreColor EMPTY = new CoreColor(null, -1);

    private final TextColor custom;
    private final int index;

    public static CoreColor fromInteger(int colorValue) {
        if (colorValue < 0) return empty();
        if (colorValue <= 0xffffff) return fromCustom(TextColor.color(colorValue));
        return fromIndex((colorValue & 0xff000000) - 1);
    }

    public static CoreColor empty() {
        return EMPTY;
    }

    public static CoreColor fromCustom(@Nullable TextColor custom) {
        return custom == null ? empty() : new CoreColor(custom, -1);
    }

    public static CoreColor fromIndex(int index) {
        return index <= -1 ? empty() : new CoreColor(null, index);
    }

    public int toInteger() {
        return isCustom() ? custom.value() : (isIndex() ? (index + 1) << 24 : -1);
    }

    public boolean isCustom() {
        return custom != null;
    }

    public boolean isIndex() {
        return index != -1;
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

}
