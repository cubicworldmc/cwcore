package space.cubicworld.core.command;

import com.velocitypowered.api.command.CommandSource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;

@Getter
@RequiredArgsConstructor
public class VelocityCoreCommandSource implements CoreCommandSource {

    private final CommandSource source;

    @Override
    public void sendMessage(Component component) {
        source.sendMessage(component);
    }

    @Override
    public TriState getPermission(String permission) {
        return switch (source.getPermissionValue(permission)) {
            case TRUE -> TriState.TRUE;
            case FALSE -> TriState.FALSE;
            case UNDEFINED -> TriState.NOT_SET;
        };
    }
}
