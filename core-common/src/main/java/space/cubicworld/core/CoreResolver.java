package space.cubicworld.core;

import net.kyori.adventure.text.format.TextColor;
import reactor.core.publisher.Mono;
import space.cubicworld.core.color.CoreColor;
import space.cubicworld.core.database.CorePlayer;

public interface CoreResolver {

    Mono<TextColor> resolve(CorePlayer player, CoreColor color);

    Mono<Long> getTeamLimit(long upgradeLevel);

}
