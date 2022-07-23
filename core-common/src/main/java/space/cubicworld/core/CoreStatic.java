package space.cubicworld.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.format.TextColor;
import space.cubicworld.core.json.ColorDeserializer;
import space.cubicworld.core.json.ColorSerializer;

import java.util.Locale;

@UtilityClass
public class CoreStatic {

    public final String CWCORE_KEY = "cwcore";
    public final String UPDATE_PLAYER = "update_player";
    public final ObjectMapper MAPPER = new ObjectMapper();
    public final Locale[] LOCALES = new Locale[]{
            Locale.ENGLISH,
            new Locale("ru")
    };
    public final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    static {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(TextColor.class, new ColorDeserializer());
        module.addSerializer(TextColor.class, new ColorSerializer());
        MAPPER.registerModule(module);
    }

}
