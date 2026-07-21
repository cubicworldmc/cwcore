package space.cubicworld.core.database.nocache;

import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;
import space.cubicworld.core.database.CoreList;
import space.cubicworld.core.database.CorePLRelation;
import space.cubicworld.core.database.CorePlayer;

import java.util.UUID;

@Data
@Builder
public class CorePLRelationImpl implements CorePLRelation {
    private final CoreNoCacheDatabase database;
    @Nullable
    private final Long id;
    private final UUID playerId;
    private final int listId;
    private Value value;

    @Override
    public Mono<? extends CorePlayer> getPlayer() {
        return database.fetchPlayer(playerId);
    }

    @Override
    public Mono<? extends CoreList> getList() {
        return database.fetchList(listId);
    }
}
