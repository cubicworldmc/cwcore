package space.cubicworld.core;

import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.properties.MapProperty;
import ch.jalu.configme.properties.Property;
import ch.jalu.configme.properties.PropertyInitializer;
import ch.jalu.configme.properties.types.PrimitivePropertyType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import space.cubicworld.core.command.ColorCommand;
import space.cubicworld.core.property.StaticComponentProperty;
import space.cubicworld.core.property.TextColorProperty;

import java.util.Map;

public class VelocityConfig implements SettingsHolder {

    public static final Property<String> SQL_HOST =
            PropertyInitializer.newProperty("sql.host", "localhost:3306");

    public static final Property<String> SQL_USERNAME =
            PropertyInitializer.newProperty("sql.username", "root");

    public static final Property<String> SQL_PASSWORD =
            PropertyInitializer.newProperty("sql.password", "1");

    public static final Property<String> SQL_DATABASE =
            PropertyInitializer.newProperty("sql.database", "cwcore");

    public static final Property<Component> COLOR_LIST =
            new StaticComponentProperty("messages.color-list", buildColorListMessageString());

    public static final Property<String> COLOR_SUCCESS =
            PropertyInitializer.newProperty("messages.color-success", "<color>Your color");
    public static final Property<Component> COLOR_NOT_VALID =
            new StaticComponentProperty(
                    "messages.color-not-valid",
                    "<red>Color is not valid :( Maybe you wanted to write color in hex format?"
            );

    public static final Property<Map<String, TextColor>> OVERWORLD_COLORS =
            new MapProperty<>(
                    "colors.overworld",
                    Map.of(
                            "default", NamedTextColor.GREEN,
                            "dark", TextColor.color(0, 255, 0),
                            "darkest", NamedTextColor.DARK_GREEN
                    ),
                    TextColorProperty.PROPERTY_TYPE
            );

    public static final Property<Map<String, TextColor>> NETHER_COLORS =
            new MapProperty<>(
                    "colors.nether",
                    Map.of(
                            "default", NamedTextColor.RED,
                            "dark", TextColor.color(255, 0, 0),
                            "darkest", NamedTextColor.DARK_RED
                    ),
                    TextColorProperty.PROPERTY_TYPE
            );

    public static final Property<Map<String, TextColor>> END_COLORS =
            new MapProperty<>(
                    "colors.end",
                    Map.of(
                            "lighter", NamedTextColor.LIGHT_PURPLE,
                            "light", TextColor.color(255, 0, 255),
                            "default", NamedTextColor.DARK_PURPLE
                    ),
                    TextColorProperty.PROPERTY_TYPE
            );

    public static final Property<String> WORLD_COLORS_MESSAGE =
            PropertyInitializer.newProperty(
                    "messages.world-colors",
                    """
                            <bold><green>Overworld</bold><gray>:
                            <overworld_default> <overworld_dark> <overworld_darkest>
                            <bold><red>Nether</bold><gray>:
                            <nether_default> <nether_dark> <nether_darkest>
                            <bold><dark_purple>End</bold><gray>:
                            <end_lighter> <end_light> <end_default>
                            """
            );

    private static String buildColorListMessageString() {
        StringBuilder builder = new StringBuilder("<white><italic>Choose one of the colors:<reset>");
        int i = 0;
        for (String color : ColorCommand.DEFAULT_COLORS) {
            builder.append(
                    "<color:%s><click:run_command:/color %s>%s ".formatted(color, color, color)
            );
            if (i++ % 4 == 0) {
                builder.append('\n');
            }
        }
        if (i % 4 != 1) builder.append('\n');
        builder.append("<white>Or write /color <hex color>");
        return builder.toString();
    }

    private VelocityConfig() {

    }

}
