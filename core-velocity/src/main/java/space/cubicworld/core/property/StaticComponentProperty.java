package space.cubicworld.core.property;

import ch.jalu.configme.properties.BaseProperty;
import ch.jalu.configme.properties.convertresult.ConvertErrorRecorder;
import ch.jalu.configme.properties.types.PrimitivePropertyType;
import ch.jalu.configme.properties.types.PropertyType;
import ch.jalu.configme.resource.PropertyReader;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.Nullable;

@Getter
public class StaticComponentProperty extends BaseProperty<Component> {

    public static final PropertyType<Component> PROPERTY_TYPE = new PropertyType<>() {
        @Nullable
        @Override
        public Component convert(@Nullable Object object, ConvertErrorRecorder errorRecorder) {
            try {
                return object == null ? null : MiniMessage.miniMessage().deserialize(object.toString());
            } catch (Exception e) {
                errorRecorder.setHasError(e.getMessage());
            }
            return null;
        }

        @Override
        public Object toExportValue(Component value) {
            return MiniMessage.miniMessage().serialize(value);
        }
    };

    public StaticComponentProperty(String path, String defaultValue) {
        this(path, MiniMessage.miniMessage().deserialize(defaultValue));
    }

    public StaticComponentProperty(String path, Component defaultValue) {
        super(path, defaultValue);
    }

    @Nullable
    @Override
    protected Component getFromReader(PropertyReader reader, ConvertErrorRecorder errorRecorder) {
        try {
            String value = reader.getString(getPath());
            if (value != null) {
                return MiniMessage.miniMessage().deserialize(value);
            } else {
                errorRecorder.setHasError("value is null");
            }
        } catch (Throwable e) {
            errorRecorder.setHasError(e.getMessage());
        }
        return null;
    }

    @Nullable
    @Override
    public Object toExportValue(Component value) {
        return MiniMessage.miniMessage().serialize(value);
    }
}
