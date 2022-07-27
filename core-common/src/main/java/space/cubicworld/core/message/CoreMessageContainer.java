package space.cubicworld.core.message;

import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import space.cubicworld.core.CorePlugin;
import space.cubicworld.core.CoreStatic;
import space.cubicworld.core.model.CorePlayer;
import space.cubicworld.core.model.CoreTeam;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
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

    public Component teamAbout(CoreTeam team, CorePlayer owner, List<CorePlayer> members) {
        return Component.empty()
                .append(Component
                        .translatable("cwcore.team.title")
                        .args(Component
                                .text(team.getName())
                                .color(NamedTextColor.GOLD)
                        )
                )
                .append(Component.newline())
                .append(Component
                        .translatable("cwcore.team.owner")
                        .args(playerMarked(owner))
                )
                .append(Component.newline())
                .append(Component
                        .translatable("cwcore.team.members")
                        .args(Component.join(
                                Component.text(", "),
                                members
                                        .stream()
                                        .map(this::playerMarked)
                                        .collect(Collectors.toList())
                        ))
                )
                .append(Component.newline())
                .append(team.getDescription() == null ?
                        Component.empty() :
                        Component.text(team.getDescription())
                );
    }

    public Component clickHere() {
        return Component.empty()
                .append(Component.text("[").color(NamedTextColor.GRAY))
                .append(Component.translatable("cwcore.click.here").color(NamedTextColor.GOLD))
                .append(Component.text("]").color(NamedTextColor.GRAY));
    }

    public Component newApplication(String teamName) {
        return Component.empty()
                .append(Component
                    .translatable("cwcore.team.approve.notification")
                )
                .append(Component.space())
                .append(clickHere()
                        .clickEvent(ClickEvent.runCommand("/team approve " + teamName))
                );
    }

    public Component applicationSent() {
        return Component
                .translatable("cwcore.team.approve.sent");
    }

    public Component application(String teamName, CorePlayer owner) {
        return Component
                .translatable("cwcore.team.approve.object")
                .args(
                        Component.text(teamName).color(NamedTextColor.GOLD),
                        playerMarked(owner)
                );
    }

}
