package space.cubicworld.core.database;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CoreTeam {

    int getId();

    @NotNull
    String getName();

    void setName(@NotNull String name);

    @Nullable
    String getDescription();

    void setDescription(@Nullable String description);

    Mono<? extends CorePlayer> getOwner();

    @NotNull
    UUID getOwnerId();

    void setOwner(@NotNull CorePlayer owner);

    boolean isHide();

    void setHide(boolean hide);

    boolean isVerified();

    void setVerified(boolean verified);

    Flux<? extends CorePlayer> getRelations(CorePTRelation.Value value, long count);

    Flux<? extends CorePlayer> getRelations(CorePTRelation.Value value, long count, long skip);

    default Flux<? extends CorePlayer> getAllRelations(CorePTRelation.Value value) {
        return getRelations(value, -1);
    }

    Mono<Long> getRelationsCount(CorePTRelation.Value value);

    Flux<? extends CoreBoost> getBoosts();

    Mono<Long> getUpgradeLevel();

    Mono<Long> getMaxMembers();

    Mono<Long> getReputation();

}
