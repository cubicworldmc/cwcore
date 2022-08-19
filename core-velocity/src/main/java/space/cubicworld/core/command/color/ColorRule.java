package space.cubicworld.core.command.color;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.format.TextColor;
import space.cubicworld.core.util.IntegerCompare;

public interface ColorRule {

    boolean isMatch(Player player);

    TextColor getColor();

}
