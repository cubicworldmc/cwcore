package space.cubicworld.core.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import space.cubicworld.core.VelocityConfig;
import space.cubicworld.core.VelocityPlugin;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class WorldColorCommand implements SimpleCommand {

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
        Map<String, List<TextColor>> worldColors = plugin
                .getConfig()
                .getProperty(VelocityConfig.WORLD_COLORS);
        String[] args = invocation.arguments();
        if (args.length != 2) {
            player.sendMessage(plugin.getMessageContainer().render(
                    plugin.getMessageContainer().worldColors(worldColors),
                    player.getPlayerSettings().getLocale()
            ));
        } else {
            List<TextColor> colors = worldColors.get(args[0]);
            if (colors == null) return;
            try {
                int index = Integer.parseInt(args[1]);
                if (colors.size() <= index) return;
                TextColor color = colors.get(index);
                plugin.getPlayerUpdater().update(player, args[0] + "Color", color);
                player.sendMessage(plugin.getMessageContainer().render(
                        plugin.getMessageContainer().worldColorSuccess(args[0], color),
                        player.getPlayerSettings().getLocale()
                ));
            } catch (NumberFormatException e) {
                /* IGNORED */
            }
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("cwcore.wcolor");
    }
}
