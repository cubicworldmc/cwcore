package space.cubicworld.core.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CoreJsonObjectMapper {

    public final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @SneakyThrows
    public byte[] writeBytes(Object obj) {
        return OBJECT_MAPPER.writeValueAsBytes(obj);
    }

    @SneakyThrows
    public <T> T readBytes(byte[] bytes, Class<T> clazz) {
        return OBJECT_MAPPER.readValue(bytes, clazz);
    }

}
