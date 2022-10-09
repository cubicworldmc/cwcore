package space.cubicworld.core;

import com.velocitypowered.api.permission.Tristate;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.format.TextColor;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import reactor.core.publisher.Mono;
import space.cubicworld.core.color.CoreColor;
import space.cubicworld.core.database.CorePlayer;

import java.util.Optional;

@RequiredArgsConstructor
public class VelocityCoreResolver implements CoreResolver {

    private final VelocityPlugin plugin;

    @Override
    public Mono<TextColor> resolve(CorePlayer player, CoreColor color) {
        if (color.isIndex()) {
            return Mono.justOrEmpty(plugin
                    .getCore()
                    .getColorIndexContainer()
                    .getColor(color.getIndex(), player)
            );
        }
        boolean premiumCounts = plugin.getConfig().getOrElse("premium.custom-color", true);
        return plugin.getServer()
                .getPlayer(player.getId())
                .filter(it -> it.getPermissionValue("cwcore.color.custom") == Tristate.TRUE)
                .map(it -> Mono.justOrEmpty(color.getCustom()))
                .orElseGet(() -> premiumCounts ? Mono.empty() :
                        plugin.getDatabase().fetchPlayerBoostsCount(player.getId())
                                .filter(boostsCount -> boostsCount != 0)
                                .map(it -> color.getCustom())
                );
    }

    @Override
    public Mono<Long> getTeamLimit(long upgradeLevel) {
        return Mono.just(15 + upgradeLevel * 5);
    }
}
