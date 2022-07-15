package space.cubicworld.core.parser;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.format.TextColor;
import space.cubicworld.core.CoreDataValue;
import space.cubicworld.core.util.Pair;

import java.lang.reflect.Field;
import java.util.Map;

@UtilityClass
public class CoreSerializer {

    private final Gson GSON = new Gson();

    @SafeVarargs
    public String write(Pair<String, Object>... objects) {
        JsonObject returnObject = new JsonObject();
        for (Pair<String, Object> object : objects) {
            String key = object.getFirst();
            Object value = object.getSecond();
            if (value instanceof String) returnObject.addProperty(key, value.toString());
            else if (value instanceof Number) returnObject.addProperty(key, (Number) value);
            else if (value instanceof Boolean) returnObject.addProperty(key, (Boolean) value);
            else if (value instanceof Character) returnObject.addProperty(key, (Character) value);
            else if (value instanceof TextColor) returnObject.addProperty(key, CoreDataValue.toValue((TextColor) value));
            else throw new IllegalArgumentException("Value with unknown to us class %s".formatted(value));
        }
        return GSON.toJson(returnObject);
    }

    public void readInto(Object into, String json) throws Exception {
        JsonObject jsonObject = GSON.fromJson(json, JsonObject.class);
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            JsonElement element = entry.getValue();
            Field field = into.getClass().getDeclaredField(key);
            boolean wasAccessible = field.canAccess(into);
            if (!wasAccessible) field.setAccessible(true);
            Class<?> fieldType = field.getType();
            Object obj;
            if (fieldType.isAssignableFrom(Number.class)) obj = element.getAsNumber();
            else if (fieldType.equals(boolean.class) || fieldType.equals(Boolean.class)) obj = element.getAsBoolean();
            else if (fieldType.equals(char.class) || fieldType.equals(Character.class)) obj = element.getAsString().charAt(0);
            else if (fieldType.isAssignableFrom(CharSequence.class)) obj = element.getAsString();
            else if (fieldType.equals(TextColor.class)) obj = CoreDataValue.getColor(element.getAsInt());
            else throw new IllegalArgumentException("Field with unknown to us class %s".formatted(fieldType));
            field.set(into, obj);
            if (!wasAccessible) field.setAccessible(false);
        }
    }

    public Object getPrimitive(Object value) {
        if (value instanceof TextColor) return CoreDataValue.toValue((TextColor) value);
        return value;
    }

}
