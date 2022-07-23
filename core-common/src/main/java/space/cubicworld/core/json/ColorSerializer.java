package space.cubicworld.core.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.kyori.adventure.text.format.TextColor;

import java.io.IOException;

public final class ColorSerializer extends JsonSerializer<TextColor> {

    @Override
    public void serialize(TextColor value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) gen.writeNull();
        else gen.writeNumber(value.value());
    }

}
