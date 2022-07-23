package space.cubicworld.core.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import net.kyori.adventure.text.format.TextColor;

import java.io.IOException;

public final class ColorDeserializer extends JsonDeserializer<TextColor> {

    @Override
    public TextColor deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_NULL) return null;
        return TextColor.color(p.getValueAsInt());
    }
}
