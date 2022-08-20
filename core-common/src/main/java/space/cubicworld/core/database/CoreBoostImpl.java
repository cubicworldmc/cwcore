package space.cubicworld.core.database;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Getter(onMethod_ = @Synchronized("lock"))
@Setter(onMethod_ = @Synchronized("lock"))
public class CoreBoostImpl implements CoreBoost {

    private final Object lock = new Object[0];

    private final CoreDatabaseImpl database;
    @Getter
    private final long id;
    @Getter
    private final UUID playerId;

    private long end;
    private Integer teamId;

    @Override
    public @NotNull CorePlayer getPlayer() {
        return database.fetchPlayer(getPlayerId())
                .orElseThrow();
    }

    @Override
    public @Nullable CoreTeam getTeam() {
        synchronized (lock) {
            return teamId == null ?
                    null :
                    database.fetchTeam(teamId)
                            .orElseGet(() -> {
                                this.teamId = null;
                                return null;
                            });
        }
    }

    @Override
    public void toTeam(int teamId) {
        this.clear();
        synchronized (lock) {
            this.teamId = teamId;
            Set<Long> list = database.getBoostCache().getTeamBoostsIfPresent(teamId);
            if (list != null) list.add(id);
        }
    }

    @Override
    public void clear() {
        synchronized (lock) {
            if (teamId != null) {
                Set<Long> list = database.getBoostCache().getTeamBoostsIfPresent(teamId);
                if (list != null) list.remove(id);
                teamId = null;
            }
        }
    }

    @Override
    public void extend() {
        synchronized (lock) {
            end += CoreBoostCache.MONTH;
        }
    }
}
