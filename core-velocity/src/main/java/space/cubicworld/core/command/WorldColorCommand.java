package space.cubicworld.core.command;

import ch.jalu.configme.properties.Property;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import space.cubicworld.core.VelocityConfig;
import space.cubicworld.core.VelocityPlugin;

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
        String[] args = invocation.arguments();
        if (args.length != 2) {
            TagResolver.Builder tagBuilder = TagResolver.builder();
            addTags(tagBuilder, "overworld", plugin
                    .getConfig()
                    .getProperty(VelocityConfig.OVERWORLD_COLORS)
            );
            addTags(tagBuilder, "nether", plugin
                    .getConfig()
                    .getProperty(VelocityConfig.NETHER_COLORS)
            );
            addTags(tagBuilder, "end", plugin
                    .getConfig()
                    .getProperty(VelocityConfig.END_COLORS)
            );
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    plugin.getConfig().getProperty(VelocityConfig.WORLD_COLORS_MESSAGE),
                    tagBuilder.build()
            ));
        }
        else {
            String world = args[0];
            String colorName = args[1];
            Property<Map<String, TextColor>> property = switch (world) {
                case "overworld" -> VelocityConfig.OVERWORLD_COLORS;
                case "nether" -> VelocityConfig.NETHER_COLORS;
                case "end" -> VelocityConfig.END_COLORS;
                default -> null;
            };
            if (property == null) return;
            Map<String, TextColor> colors = plugin.getConfig().getProperty(property);
            TextColor color = colors.get(colorName);
            if (color == null) return;
            plugin.getPlayerUpdater().update(player, world + "Color", color);
        }
    }

    private void addTags(TagResolver.Builder tagBuilder, String name, Map<String, TextColor> colors) {
        colors.forEach((colorName, color) -> tagBuilder
                .tag(
                        "%s_%s".formatted(name, colorName),
                        Tag.inserting(Component
                                .text(colorName)
                                .color(color)
                                .clickEvent(ClickEvent.runCommand(
                                        "/wcolor %s %s".formatted(name, colorName)
                                ))
                        )
                )
        );
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("cwcore.wcolor");
    }
}
