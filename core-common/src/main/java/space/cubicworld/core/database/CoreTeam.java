package space.cubicworld.core.database;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CoreTeam {

    int getId();

    @NotNull
    String getName();

    void setName(@NotNull String name);

    @Nullable
    String getDescription();

    void setDescription(@Nullable String description);

    @NotNull
    CorePlayer getOwner();

    @NotNull
    UUID getOwnerId();

    void setOwner(@NotNull CorePlayer owner);

    boolean isHide();

    void setHide(boolean hide);

    boolean isVerified();

    void setVerified(boolean verified);

    @NotNull
    List<CorePlayer> getRelations(CorePTRelation.Value value, int count);

    default List<CorePlayer> getAllRelations(CorePTRelation.Value value) {
        return getRelations(value, CoreRelationCache.ALL);
    }

}
