package space.cubicworld.core.database;

import lombok.Data;
import net.kyori.adventure.text.format.TextColor;
import space.cubicworld.core.json.CoreLightPlayer;

import java.util.UUID;

@Data
public class CoreLightPlayerData implements CoreLightPlayer {

    private final UUID id;
    private final String name;
    private final TextColor resolvedGlobalColor;

}
