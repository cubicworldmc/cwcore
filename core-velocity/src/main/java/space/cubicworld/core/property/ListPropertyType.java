package space.cubicworld.core.property;

import ch.jalu.configme.properties.convertresult.ConvertErrorRecorder;
import ch.jalu.configme.properties.types.PropertyType;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ListPropertyType<T> implements PropertyType<List<T>> {

    private final PropertyType<T> propertyType;

    @Nullable
    @Override
    public List<T> convert(@Nullable Object object, ConvertErrorRecorder errorRecorder) {
        if (!(object instanceof List<?> list)) {
            return null;
        }
        List<T> result = new ArrayList<>();
        for (Object obj : list) {
            T tObj = propertyType.convert(obj, errorRecorder);
            if (tObj == null) return null;
            result.add(tObj);
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public Object toExportValue(List<T> value) {
        return value
                .stream()
                .map(propertyType::toExportValue)
                .collect(Collectors.toList());
    }
}
