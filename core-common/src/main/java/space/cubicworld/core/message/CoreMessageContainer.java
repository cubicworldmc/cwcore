package space.cubicworld.core.message;

import lombok.AccessLevel;
import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import org.jetbrains.annotations.NotNull;
import space.cubicworld.core.CoreStatic;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Getter
public class CoreMessageContainer {

    private final CoreColorContainer colorResources;

    public CoreMessageContainer(Function<String, InputStream> resourceReader) throws IOException {
        TranslationRegistry registry = TranslationRegistry.create(Key.key("cwcore", "main"));
        registry.defaultLocale(Locale.ENGLISH);
        for (Locale locale : new Locale[]{Locale.ENGLISH, new Locale("ru")}) {
            try (InputStream localeInputStream = resourceReader.apply(
                    "language/%s.properties".formatted(locale.getLanguage()))
            ) {
                if (localeInputStream == null) {
                    CoreStatic.getLogger().warn("Translation {} is not exist", locale);
                    continue;
                }
                registry.registerAll(
                        locale,
                        new PropertyResourceBundle(new InputStreamReader(localeInputStream, StandardCharsets.UTF_8)),
                        false
                );
            } catch (IOException e) {
                CoreStatic.getLogger().warn("Failed to load translation for {}:", locale, e);
            }
        }
        GlobalTranslator.get().addSource(registry);
        colorResources = new CoreColorContainer(resourceReader.apply("colors.properties"));
    }

    public Component render(Component message, Locale locale) {
        return GlobalTranslator.render(message, locale);
    }

    @Getter(AccessLevel.PRIVATE)
    private final Map<NamedTextColor, Component> namedColors = new ConcurrentHashMap<>();

    private final Component colorList = generateColorList();

    private final Component colorNotValid = Component
            .translatable("cwcore.color.command.not-valid")
            .color(NamedTextColor.RED);

    private final Component colorSuccess = Component
            .translatable("cwcore.color.command.success");

    public @NotNull Component color(NamedTextColor color) {
        return namedColors.computeIfAbsent(color, it -> Component.translatable("cwcore.color.%s".formatted(it)));
    }

    public Component worldColors(Map<String, List<TextColor>> colors) {
        Component result = Component.empty()
                .append(Component
                        .translatable("cwcore.wcolor.command.header")
                        .decorate(TextDecoration.ITALIC)
                ).append(Component.newline());
        for (Map.Entry<String, List<TextColor>> entry : colors.entrySet()) {
            String worldName = entry.getKey();
            Collection<TextColor> worldColors = entry.getValue();
            result = result.append(Component
                    .translatable("cwcore.world.%s".formatted(worldName))
                    .color(colorResources.getWorld(worldName))
            ).append(Component.newline());
            int variant = 1;
            for (TextColor worldColor : worldColors) {
                result = result.append(Component
                        .translatable("cwcore.wcolor.command.variant")
                        .args(Component
                                .text(Integer.toString(variant))
                        )
                        .clickEvent(ClickEvent
                                .runCommand("/wcolor %s %s".formatted(worldName, (variant++ - 1)))
                        )
                        .color(worldColor)
                ).append(Component.space());
            }
            result = result.append(Component.newline());
        }
        return result;
    }

    public Component worldColorSuccess(String world, TextColor color) {
        return Component
                .translatable("cwcore.wcolor.command.success")
                .args(Component.text(world))
                .color(color);
    }

    private Component generateColorList() {
        Component result = Component.empty()
                .append(Component
                        .translatable("cwcore.color.command.header")
                        .decorate(TextDecoration.ITALIC)
                );
        int counter = 0;
        for (NamedTextColor color : NamedTextColor.NAMES.values()) {
            result = result.append(color(color)
                    .color(color)
                    .clickEvent(ClickEvent.runCommand("/color %s".formatted(color.toString())))
                    .hoverEvent(Component
                            .translatable("cwcore.color.command.hover")
                            .args(color(color))
                    )
            );
            if (counter++ % 4 == 0) result = result.append(Component.newline());
        }
        if (counter % 4 != 0) result = result.append(Component.newline());
        return result.append(Component.translatable("cwcore.color.command.footer"));
    }

}