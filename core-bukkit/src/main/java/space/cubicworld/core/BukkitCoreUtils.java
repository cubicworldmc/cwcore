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
        StringBuilder hexBuilder = new StringBuilder();
        for (char c : color.asHexString().toCharArray()) {
            hexBuilder.append(ChatColor.COLOR_CHAR).append(c);
        }
        return hexBuilder.toString();
    }

    public TextColor getColorNamed(String color) {
        if (color == null) return NamedTextColor.WHITE;
        TextColor textColor = NamedTextColor.NAMES.value(color.toLowerCase(Locale.ROOT));
        if (textColor != null) return textColor;
        return TextColor.fromCSSHexString(color);
    }

    public void internalPlayerKick(Player player) {
        if (!player.isOnline()) return;
        player.kick(Component
                .text("Internal error happened. Connect administrator")
                .color(NamedTextColor.RED)
        );
    }

}
