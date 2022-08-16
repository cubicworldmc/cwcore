package space.cubicworld.core.database;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Data
@Getter(onMethod_ = @Synchronized("lock"))
@Setter(onMethod_ = @Synchronized("lock"))
class CorePTRelationImpl implements CorePTRelation {

    private final Object lock = new Object[0];

    private final CoreDatabaseImpl database;

    private final UUID playerId;
    private final int teamId;
    private Value value;

    @Override
    public @NotNull CorePlayer getPlayer() {
        synchronized (lock) {
            return database.fetchPlayer(playerId)
                    .orElseThrow(() -> new IllegalArgumentException("Passed bad player id"));
        }
    }

    @Override
    public @NotNull CoreTeam getTeam() {
        synchronized (lock) {
            return database.fetchTeam(teamId)
                    .orElseThrow(() -> new IllegalArgumentException("Passed bad team id"));
        }
    }

    public void setValue(@NotNull Value value) {
        synchronized (lock) {
            if (this.value != null) {
                database.getRelationCache().changeRelationValue(playerId, teamId, value, this.value);
            }
            this.value = value;
        }
    }
}
