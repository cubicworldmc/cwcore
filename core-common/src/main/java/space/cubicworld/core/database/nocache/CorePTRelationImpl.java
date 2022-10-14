package space.cubicworld.core.database.nocache;

import lombok.Builder;
import lombok.Data;
import reactor.core.publisher.Mono;
import space.cubicworld.core.database.CorePTRelation;
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.database.CoreTeam;

import java.util.UUID;

@Data
@Builder
class CorePTRelationImpl implements CorePTRelation {

    private final CoreNoCacheDatabase database;
    private final UUID playerId;
    private final int teamId;
    private Value value;

    @Override
    public Mono<? extends CorePlayer> getPlayer() {
        return database.fetchPlayer(playerId);
    }

    @Override
    public Mono<? extends CoreTeam> getTeam() {
        return database.fetchTeam(teamId);
    }

}
