package space.cubicworld.core.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import space.cubicworld.core.VelocityPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class ColorCommand implements SimpleCommand {

    public static final List<String> DEFAULT_COLORS = new ArrayList<>(NamedTextColor.NAMES.keys());
    public static final Pattern HEX_PATTERN = Pattern.compile("#[\\da-fA-F]{3,6}");

    private final VelocityPlugin plugin;

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(Component
                    .text("Only for players")
                    .color(NamedTextColor.RED)
            );
            return;
        }
        String[] args = invocation.arguments();
        if (args.length == 0) {
            player.sendMessage(plugin.getMessageContainer().render(
                    plugin.getMessageContainer().getColorList(),
                    player.getPlayerSettings().getLocale()
            ));
            return;
        }
        Matcher patternMatcher = HEX_PATTERN.matcher(args[0]);
        TextColor color;
        if (patternMatcher.find()) {
            color = TextColor.fromCSSHexString(args[0]);
        } else {
            color = NamedTextColor.NAMES.value(args[0].toLowerCase(Locale.ROOT));
        }
        if (color == null) {
            player.sendMessage(plugin.getMessageContainer().render(
                    plugin.getMessageContainer().getColorNotValid(),
                    player.getPlayerSettings().getLocale()
            ));
            return;
        }
        plugin.getPlayerUpdater().update(player, "globalColor", color.value());
        player.sendMessage(plugin.getMessageContainer().render(
                plugin.getMessageContainer().getColorSuccess(),
                player.getPlayerSettings().getLocale()
        ));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("cwcore.color");
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length <= 1) {
            return DEFAULT_COLORS;
        }
        return Collections.emptyList();
    }
}
