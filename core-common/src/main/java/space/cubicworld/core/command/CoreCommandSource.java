package space.cubicworld.core.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;

public interface CoreCommandSource {

    void sendMessage(Component component);

    TriState getPermission(String permission);

}
