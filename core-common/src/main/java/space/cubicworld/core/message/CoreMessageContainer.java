package space.cubicworld.core.message;

import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import space.cubicworld.core.CorePlugin;
import space.cubicworld.core.CoreStatic;
import space.cubicworld.core.model.CorePlayer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.PropertyResourceBundle;
import java.util.stream.Collectors;

public class CoreMessageContainer {

    @Getter
    private final TranslationRegistry registry;

    public CoreMessageContainer(CorePlugin plugin) {
        registry = TranslationRegistry
                .create(Key.key(CoreStatic.CWCORE_KEY, "main"));
        registry.defaultLocale(CoreStatic.DEFAULT_LOCALE);
        for (Locale locale : CoreStatic.LOCALES) {
            try (InputStream localeInputStream = plugin.readResource("translation/%s.properties".formatted(locale))) {
                if (localeInputStream == null) {
                    plugin.getLogger().warn("Translation {} is not exist", locale);
                    continue;
                }
                registry.registerAll(
                        locale,
                        new PropertyResourceBundle(new InputStreamReader(localeInputStream, StandardCharsets.UTF_8)),
                        false
                );
            } catch (IOException e) {
                plugin.getLogger().warn("Failed to read translation {}:", locale, e);
            }
        }
        GlobalTranslator.get().addSource(registry);
    }

    public Component renderChat(Component toRender, Locale locale) {
        return renderExternal(
                Component.empty()
                        .append(Component
                                .text("[")
                                .color(NamedTextColor.GRAY)
                        )
                        .append(Component
                                .text("Cubic")
                                .color(NamedTextColor.GREEN)
                        )
                        .append(Component
                                .text("]")
                                .color(NamedTextColor.GRAY)
                        )
                        .append(Component
                                .text(":")
                                .color(NamedTextColor.GOLD)
                        )
                        .append(Component.newline())
                        .append(toRender),
                locale
        );
    }

    public Component renderExternal(Component toRender, Locale locale) {
        return GlobalTranslator.render(toRender, locale);
    }

    public Component unknownCommand(String command, Collection<String> available) {
        return Component.empty()
                .append(Component
                        .translatable("cwcore.command.node.unknown")
                        .color(NamedTextColor.RED)
                        .args(Component.text(command))
                )
                .append(Component.newline())
                .append(availableCommands(available));
    }

    public Component availableCommands(Collection<String> available) {
        return Component.empty()
                .append(Component.translatable("cwcore.command.node.available"))
                .append(Component.newline())
                .append(Component.join(
                        Component.newline(),
                        available
                                .stream()
                                .map(command -> Component.empty()
                                        .append(Component
                                                .text("-")
                                                .color(NamedTextColor.GRAY)
                                        )
                                        .append(Component.space())
                                        .append(Component
                                                .text(command)
                                                .color(NamedTextColor.GOLD)
                                        )
                                )
                                .collect(Collectors.toList())
                ));
    }

    public Component unknownPlayer(String nickname) {
        return Component
                .translatable("cwcore.player.unknown")
                .color(NamedTextColor.RED)
                .args(Component.text(nickname));
    }

    public Component providePlayer() {
        return Component
                .translatable("cwcore.player.provide")
                .color(NamedTextColor.RED);
    }

    public Component seeReputation(Component name, int reputation) {
        return Component
                .translatable("cwcore.command.rep.see")
                .args(
                        name,
                        Component
                                .text(Integer.toString(reputation))
                                .color(NamedTextColor.GOLD)
                );
    }

    public Component playerMarked(CorePlayer player) {
        return Component
                .text(player.getName())
                .color(Objects.requireNonNullElse(player.getGlobalColor(), NamedTextColor.GOLD))
                .hoverEvent(HoverEvent.showEntity(
                        Key.key("minecraft", "player"),
                        player.getUuid()
                ));
    }

}
