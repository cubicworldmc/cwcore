package space.cubicworld.core;

import com.velocitypowered.api.permission.Tristate;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.format.TextColor;
import space.cubicworld.core.color.CoreColor;
import space.cubicworld.core.database.CorePlayer;

@RequiredArgsConstructor
public class VelocityCoreResolver implements CoreResolver {

    private final VelocityPlugin plugin;

    @Override
    public TextColor resolve(CorePlayer player, CoreColor color) {
        if (color.isIndex()) {
            return plugin.getCore()
                    .getColorIndexContainer()
                    .getColor(color.getIndex(), player)
                    .orElse(null);
        }
        return plugin.getServer()
                .getPlayer(player.getId())
                .filter(it -> it.getPermissionValue("cwcore.color.custom") == Tristate.TRUE)
                .map(it -> color.getCustom())
                .orElse(null);
    }
}
