package space.cubicworld.core;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.format.TextColor;

@UtilityClass
public class CoreDataValue {

    public TextColor getColor(int value) {
        return value == -1 ? null : TextColor.color(value);
    }

    public int toValue(TextColor color) {
        return color == null ? -1 : color.value();
    }

    public String getSQLName(String name) {
        StringBuilder builder = new StringBuilder();
        for (char ch: name.toCharArray()) {
            if (Character.isUpperCase(ch)) builder.append("_").append(Character.toLowerCase(ch));
            else builder.append(ch);
        }
        return builder.toString();
    }

}
