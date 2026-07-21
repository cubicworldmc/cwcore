package space.cubicworld.core.database;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CorePLRelation {
    enum Value {
        PENDING,
        DECLINED,
        ACCEPTED,
        NONE;
    }

    @Nullable
    Long getId();

    Mono<? extends CorePlayer> getPlayer();

    Mono<? extends CoreList> getList();

    UUID getPlayerId();

    int getListId();

    Value getValue();

    void setValue(@NotNull Value value);
}