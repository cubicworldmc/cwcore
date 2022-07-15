package space.cubicworld.core;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Locale;

@UtilityClass
public class BukkitCoreUtils {

    public String toColorCode(TextColor color) {
        if (color == null) return null;
        if (color instanceof NamedTextColor) {
            return ChatColor
                    .valueOf(color.toString().toUpperCase(Locale.ROOT))
                    .toString();
        }
        StringBuilder hexBuilder = new StringBuilder()
                .append(ChatColor.COLOR_CHAR)
                .append('x');
        for (char c : String.format("%06x", color.value()).toCharArray()) {
            hexBuilder.append(ChatColor.COLOR_CHAR).append(c);
        }
        return hexBuilder.toString();
    }

    public void internalPlayerKick(Player player) {
        if (player == null || !player.isOnline()) return;
        player.kick(Component
                .text("Internal error happened. Connect administrator")
                .color(NamedTextColor.RED)
        );
    }

}
