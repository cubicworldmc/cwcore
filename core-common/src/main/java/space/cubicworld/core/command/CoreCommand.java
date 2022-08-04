package space.cubicworld.core.command;

import net.kyori.adventure.util.TriState;

import java.util.Iterator;
import java.util.List;

public interface CoreCommand<S> {

    void execute(S source, Iterator<String> args);

    List<String> tab(S source, Iterator<String> args);

    default String getName() {
        CoreCommandAnnotation annotation = getClass().getAnnotation(CoreCommandAnnotation.class);
        if (annotation == null || annotation.name().isEmpty()) {
            String name = getClass().getSimpleName();
            return name.endsWith("Command") ?
                    name.substring(0, name.length() - "Command".length()) :
                    name;
        }
        return annotation.name();
    }

    default String[] getAliases() {
        CoreCommandAnnotation annotation = getClass().getAnnotation(CoreCommandAnnotation.class);
        return annotation == null ? new String[0] : annotation.aliases();
    }

    default String getPermission() {
        CoreCommandAnnotation annotation = getClass().getAnnotation(CoreCommandAnnotation.class);
        if (annotation == null || annotation.permission().isEmpty()) {
            StringBuilder permission = new StringBuilder();
            getName().chars().forEachOrdered(c -> {
                if (Character.isUpperCase(c)) {
                    permission.append(Character.toLowerCase(c)).append('_');
                } else {
                    permission.append(c);
                }
            });
            return permission.toString();
        }
        return annotation.permission();
    }

    default boolean isAdmin() {
        CoreCommandAnnotation annotation = getClass().getAnnotation(CoreCommandAnnotation.class);
        return annotation == null || annotation.admin();
    }

    default boolean hasPermission(TriState state) {
        return switch (state) {
            case TRUE -> true;
            case FALSE -> false;
            case NOT_SET -> !isAdmin();
        };
    }

}
