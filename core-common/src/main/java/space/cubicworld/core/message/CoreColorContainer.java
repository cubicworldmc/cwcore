package space.cubicworld.core.message;

import net.kyori.adventure.text.format.TextColor;
import space.cubicworld.core.CoreUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CoreColorContainer extends CoreProperties<TextColor> {

    private final Map<String, TextColor> worlds = new ConcurrentHashMap<>();

    public CoreColorContainer(InputStream is) throws IOException {
        super(is, CoreUtils::getColorNamed);
    }

    public TextColor getWorld(String world) {
        return worlds.computeIfAbsent(world, worldKey -> get("world.%s".formatted(worldKey)));
    }

}
