package space.cubicworld.core.message;

import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;

public abstract class CoreProperties<T> {

    private final Properties properties = new Properties();
    private final Function<String, T> function;

    public CoreProperties(InputStream is, Function<String, T> function) throws IOException {
        properties.load(is);
        this.function = function;
    }

    protected T get(String path) {
        return function.apply(Objects.requireNonNull(
                properties.getProperty(path),
                "Property %s is not exist".formatted(path)
        ));
    }

}
