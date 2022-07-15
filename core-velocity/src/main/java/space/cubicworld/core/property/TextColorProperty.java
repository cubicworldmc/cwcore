package space.cubicworld.core.property;

import ch.jalu.configme.properties.BaseProperty;
import ch.jalu.configme.properties.convertresult.ConvertErrorRecorder;
import ch.jalu.configme.properties.types.PropertyType;
import ch.jalu.configme.resource.PropertyReader;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Nullable;
import space.cubicworld.core.CoreDataValue;
import space.cubicworld.core.CoreUtils;

public class TextColorProperty extends BaseProperty<TextColor> {

    public static final PropertyType<TextColor> PROPERTY_TYPE = new PropertyType<>() {
        @Nullable
        @Override
        public TextColor convert(@Nullable Object object, ConvertErrorRecorder errorRecorder) {
            return object instanceof Number ? CoreDataValue.getColor(((Number) object).intValue()) : null;
        }

        @Override
        public Object toExportValue(TextColor value) {
            return CoreDataValue.toValue(value);
        }
    };

    public TextColorProperty(String path, TextColor defaultValue) {
        super(path, defaultValue);
    }

    @Nullable
    @Override
    protected TextColor getFromReader(PropertyReader reader, ConvertErrorRecorder errorRecorder) {
        try {
            return CoreUtils.getColorNamed(reader.getString(getPath()));
        } catch (Exception e) {
            errorRecorder.setHasError(e.getMessage());
        }
        return null;
    }

    @Nullable
    @Override
    public Object toExportValue(TextColor value) {
        return value.toString();
    }
}
