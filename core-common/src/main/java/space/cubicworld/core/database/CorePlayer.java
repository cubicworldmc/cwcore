package space.cubicworld.core.database;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.cubicworld.core.color.CoreColor;
import space.cubicworld.core.json.CoreLightPlayer;

import java.util.List;

public interface CorePlayer extends CoreLightPlayer {

    void setName(@NotNull String name);

    int getReputation();

    void setReputation(int reputation);

    @NotNull
    CoreColor getGlobalColor();

    void setGlobalColor(@NotNull CoreColor color);

    @Nullable
    CoreTeam getSelectedTeam();

    @Nullable
    Integer getSelectedTeamId();

    void setSelectedTeam(@Nullable CoreTeam team);

    @NotNull List<CoreTeam> getRelations(CorePTRelation.Value value, int count);

    default List<CoreTeam> getAllRelations(CorePTRelation.Value value) {
        return getRelations(value, CoreRelationCache.ALL);
    }

    int getRelationsCount(CorePTRelation.Value value);

    List<CoreBoost> getBoosts();

    void setInactiveBoosts(int boosts);

    int getInactiveBoosts();

    void incrementInactiveBoosts();

    void decrementInactiveBoosts();

}
