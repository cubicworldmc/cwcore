package space.cubicworld.core.database.nocache;

import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;
import space.cubicworld.core.CoreStatic;
import space.cubicworld.core.database.CoreBoost;
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.database.CoreTeam;

import java.util.UUID;

@Data
@Builder
class CoreBoostImpl implements CoreBoost {

    private final CoreNoCacheDatabase database;
    private final long id;
    private final UUID playerId;
    private long end;
    private @Nullable Integer teamId;

    @Override
    public Mono<? extends CorePlayer> getPlayer() {
        return database.fetchPlayer(playerId);
    }

    @Override
    public Mono<? extends CoreTeam> getTeam() {
        return teamId == null ? Mono.empty() : database.fetchTeam(teamId);
    }

    @Override
    public void toTeam(int teamId) {
        // clear(); -- should be used when more than one usage of boost will be added
        this.teamId = teamId;
    }

    @Override
    public void clear() {
        teamId = null;
    }

    @Override
    public void extend() {
        end = end + CoreStatic.BOOST_EXTEND_UNIX;
    }

}
