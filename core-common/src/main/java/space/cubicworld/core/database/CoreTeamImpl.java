package space.cubicworld.core.database;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Getter(onMethod_ = @Synchronized("lock"))
@Setter(onMethod_ = @Synchronized("lock"))
class CoreTeamImpl implements CoreTeam {

    private final Object lock = new Object[0];

    private final CoreDatabaseImpl database;

    private final int id;
    @NotNull
    private String name;
    @Nullable
    private String description;
    private boolean verified;
    private boolean hide;
    @NotNull
    private UUID ownerId;

    @Override
    public @NotNull CorePlayer getOwner() {
        synchronized (lock) {
            return database.fetchPlayer(ownerId)
                    .orElseThrow(() -> new IllegalArgumentException("Passed bad owner id"));
        }
    }

    @Override
    public void setOwner(@NotNull CorePlayer owner) {
        synchronized (lock) {
            ownerId = owner.getId();
        }
    }

    @Override
    public @NotNull List<CorePlayer> getRelations(CorePTRelation.Value value, int count) {
        return database
                .getRelationCache()
                .fetchPlayers(value, id, count)
                .stream()
                .map(id -> database.fetchPlayer(id).orElseThrow())
                .collect(Collectors.toList());
    }

    @Override
    public int getRelationsCount(CorePTRelation.Value value) {
        return database
                .getRelationCache()
                .fetchPlayersSize(value, id);
    }
}
