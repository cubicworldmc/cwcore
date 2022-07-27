package space.cubicworld.core;

import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.properties.Property;
import ch.jalu.configme.properties.PropertyInitializer;
import ch.jalu.configme.properties.StringListProperty;

import java.util.List;

public class VelocityConfig implements SettingsHolder {

    public static final Property<String> SQL_HOST =
            PropertyInitializer.newProperty("mysql.host", "localhost:3306");

    public static final Property<String> SQL_USERNAME =
            PropertyInitializer.newProperty("mysql.username", "root");

    public static final Property<String> SQL_PASSWORD =
            PropertyInitializer.newProperty("mysql.password", "1");

    public static final Property<String> SQL_DATABASE =
            PropertyInitializer.newProperty("mysql.database", "cwcore");

    public static final Property<List<String>> IGNORED_SERVERS =
            new StringListProperty("ignored-servers", "reg-limbo");

    private VelocityConfig() {

    }

}
