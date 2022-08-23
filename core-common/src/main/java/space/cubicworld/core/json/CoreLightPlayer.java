package space.cubicworld.core.json;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@JsonSerialize(as = CoreLightPlayerImpl.class)
@JsonDeserialize(as = CoreLightPlayerImpl.class)
public interface CoreLightPlayer {

    @NotNull
    UUID getId();

    @NotNull
    String getName();

    @Nullable
    TextColor getResolvedGlobalColor();

}
