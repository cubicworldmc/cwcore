package space.cubicworld.core.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import net.kyori.adventure.text.format.TextColor;

import java.util.UUID;

@Data
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class CoreLightPlayerImpl implements CoreLightPlayer {

    public static CoreLightPlayer toImpl(CoreLightPlayer player) {
        TextColor globalColor = player.getResolvedGlobalColor();
        return new CoreLightPlayerImpl(
                player.getId(),
                player.getName(),
                globalColor == null ? -1 : globalColor.value(),
                player.getSelectedTeamName()
        );
    }

    public static CoreLightPlayer defaultImpl(UUID id, String name) {
        return new CoreLightPlayerImpl(id, name, -1, null);
    }

    private UUID id;
    private String name;
    private int color;
    private String selectedTeamName;

    @JsonIgnore
    public TextColor getResolvedGlobalColor() {
        return color == -1 ? null : TextColor.color(color);
    }

}

