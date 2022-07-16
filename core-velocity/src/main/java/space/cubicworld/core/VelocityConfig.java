package space.cubicworld.core;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.properties.*;
import ch.jalu.configme.properties.types.PrimitivePropertyType;
import ch.jalu.configme.properties.types.PropertyType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import space.cubicworld.core.command.ColorCommand;
import space.cubicworld.core.property.ListPropertyType;
import space.cubicworld.core.property.StaticComponentProperty;
import space.cubicworld.core.property.TextColorProperty;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class VelocityConfig implements SettingsHolder {

    public static final Property<String> SQL_HOST =
            PropertyInitializer.newProperty("sql.host", "localhost:3306");

    public static final Property<String> SQL_USERNAME =
            PropertyInitializer.newProperty("sql.username", "root");

    public static final Property<String> SQL_PASSWORD =
            PropertyInitializer.newProperty("sql.password", "1");

    public static final Property<String> SQL_DATABASE =
            PropertyInitializer.newProperty("sql.database", "cwcore");

    @Comment({
            "Servers on which player won't be handled as online"
    })
    public static final Property<Set<String>> IGNORED_SERVERS = new SetProperty<>(
            "ignored.servers",
            PrimitivePropertyType.STRING,
            Set.of("reg-limbo")
    );

    public static final Property<Map<String, List<TextColor>>> WORLD_COLORS =
            new MapProperty<>(
                    "colors.worlds",
                    Map.of(
                            "overworld", List.of(
                                    NamedTextColor.GREEN,
                                    TextColor.color(0, 255, 0),
                                    NamedTextColor.DARK_GREEN
                            ),
                            "nether", List.of(
                                    NamedTextColor.RED,
                                    TextColor.color(255, 0, 0),
                                    NamedTextColor.DARK_RED
                            ),
                            "end", List.of(
                                    NamedTextColor.LIGHT_PURPLE,
                                    TextColor.color(255, 0, 255),
                                    NamedTextColor.DARK_PURPLE
                            )
                    ),
                    new ListPropertyType<>(TextColorProperty.PROPERTY_TYPE)
            );

    private VelocityConfig() {

    }

}
