package space.cubicworld.core;

import net.kyori.adventure.text.format.TextColor;
import space.cubicworld.core.color.CoreColor;
import space.cubicworld.core.database.CorePlayer;

public interface CoreResolver {

    TextColor resolve(CorePlayer player, CoreColor color);

    int getTeamLimit(int upgradeLevel);

}
