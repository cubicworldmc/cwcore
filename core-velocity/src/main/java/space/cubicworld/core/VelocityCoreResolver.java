package space.cubicworld.core;

import com.velocitypowered.api.permission.Tristate;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.format.TextColor;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import space.cubicworld.core.color.CoreColor;
import space.cubicworld.core.database.CorePlayer;

import java.util.Optional;

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
        boolean premiumCounts = plugin.getConfig().getOrElse("premium.custom-color", true);
        return plugin.getServer()
                .getPlayer(player.getId())
                .filter(it -> it.getPermissionValue("cwcore.color.custom") == Tristate.TRUE)
                .map(it -> color.getCustom())
                .orElseGet(() ->
                        !premiumCounts || plugin.getDatabase().fetchPlayerBoosts(player.getId()).isEmpty() ?
                                null : color.getCustom()
                );
    }

    @Override
    public int getTeamLimit(int upgradeLevel) {
        return 15 + upgradeLevel * 5;
    }
}
