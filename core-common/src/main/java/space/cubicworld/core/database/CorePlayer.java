package space.cubicworld.core.database;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import space.cubicworld.core.color.CoreColor;
import space.cubicworld.core.json.CoreLightPlayer;

import java.util.UUID;

public interface CorePlayer {

    UUID getId();

    String getName();

    void setName(@NotNull String name);

    int getReputation();

    void setReputation(int reputation);

    @NotNull
    CoreColor getGlobalColor();

    void setGlobalColor(@NotNull CoreColor color);

    Mono<? extends CoreTeam> getSelectedTeam();

    @Nullable
    Integer getSelectedTeamId();

    void setSelectedTeam(@Nullable CoreTeam team);

    @Nullable
    Long getDiscordId();

    void setDiscordId(@Nullable Long discordId);

    Flux<? extends CoreTeam> getRelations(CorePTRelation.Value value, long count);

    Flux<? extends CoreTeam> getRelations(CorePTRelation.Value value, long count, long skip);

    default Flux<? extends CoreTeam> getAllRelations(CorePTRelation.Value value) {
        return getRelations(value, -1);
    }

    Mono<Long> getRelationsCount(CorePTRelation.Value value);

    Flux<? extends CoreBoost> getBoosts(long count, long skip);

    Mono<Long> getBoostsCount();

    void setInactiveBoosts(int boosts);

    int getInactiveBoosts();

    Mono<? extends CoreLightPlayer> asLight();

}
