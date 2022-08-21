package space.cubicworld.core.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.util.TriState;

import java.util.Locale;

@Getter
@RequiredArgsConstructor
public class VelocityCoreCommandSource implements CoreCommandSource {

    private final CommandSource source;

    public static void sendLocaleMessage(CommandSource source, Component component) {
        source.sendMessage(GlobalTranslator.render(
                Component.newline().append(component),
                source instanceof Player player ?
                        player.getPlayerSettings().getLocale() :
                        Locale.ENGLISH
        ));
    }

    @Override
    public void sendMessage(Component component) {
        sendLocaleMessage(source, component);
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
