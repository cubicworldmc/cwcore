package space.cubicworld.core.database;

import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface CorePlayer {

    UUID getId();

    @NotNull
    String getName();

    void setName(@NotNull String name);

    int getReputation();

    void setReputation(int reputation);

    @Nullable
    TextColor getGlobalColor();

    void setGlobalColor(@Nullable TextColor color);

    @Nullable
    CoreTeam getSelectedTeam();

    @Nullable
    Integer getSelectedTeamId();

    void setSelectedTeam(@Nullable CoreTeam team);

    @NotNull List<CoreTeam> getRelations(CorePTRelation.Value value, int count);

    default List<CoreTeam> getAllRelations(CorePTRelation.Value value) {
        return getRelations(value, CoreRelationCache.ALL);
    }

}
