package space.cubicworld.core;

import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.properties.Property;
import ch.jalu.configme.properties.PropertyInitializer;
import net.kyori.adventure.text.Component;
import space.cubicworld.core.command.ColorCommand;
import space.cubicworld.core.property.StaticComponentProperty;

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

    public static final Property<Component> COLOR_NOT_CHOSEN =
            new StaticComponentProperty("messages.color-not-chosen", "<red>Please choose color");

    public static final Property<Component> COLOR_NOT_VALID =
            new StaticComponentProperty(
                    "messages.color-not-valid",
                    "<red>Color is not valid :( Maybe you wanted to write color in hex format?"
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
