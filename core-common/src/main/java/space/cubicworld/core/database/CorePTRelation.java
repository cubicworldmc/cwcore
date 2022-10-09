package space.cubicworld.core.database;

import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CorePTRelation {

    enum Value {
        MEMBERSHIP,
        INVITE,
        READ,
        NONE;

        public boolean isInvite() {
            return this == INVITE || this == READ;
        }
    }

    Mono<? extends CorePlayer> getPlayer();

    Mono<? extends CoreTeam> getTeam();

    @NotNull
    UUID getPlayerId();

    int getTeamId();

    @NotNull
    Value getValue();

    void setValue(@NotNull Value value);

}
