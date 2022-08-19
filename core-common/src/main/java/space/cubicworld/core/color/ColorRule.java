package space.cubicworld.core.color;

import net.kyori.adventure.text.Component;
import space.cubicworld.core.database.CorePlayer;

public interface ColorRule {

    boolean isMatches(CorePlayer player);

    Component getMessage();

}
