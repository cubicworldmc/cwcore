package space.cubicworld.core.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

@UtilityClass
public class MessageUtils {

    @Nullable
    public String buildMessage(Iterator<String> args) {
        StringBuilder builder = new StringBuilder();
        args.forEachRemaining(arg -> builder.append(arg).append(' '));
        if (builder.isEmpty()) return null;
        String result = builder.substring(0, builder.length() - 1);
        if (result.isBlank()) return null;
        return result;
    }

}
