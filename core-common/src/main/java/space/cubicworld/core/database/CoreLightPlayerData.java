package space.cubicworld.core.database;

import lombok.Data;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Nullable;
import space.cubicworld.core.json.CoreLightPlayer;

import java.util.UUID;

@Data
public class CoreLightPlayerData implements CoreLightPlayer {

    private final UUID id;
    private final String name;
    private final TextColor resolvedGlobalColor;
    private final @Nullable String selectedTeamPrefix;

}
