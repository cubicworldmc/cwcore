package space.cubicworld.core.database;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CoreBoost {

    long getId();

    @NotNull
    UUID getPlayerId();

    Mono<? extends CorePlayer> getPlayer();

    long getEnd();

    void setEnd(long unixTime);

    default boolean isEnd() {
        return getEnd() < System.currentTimeMillis();
    }

    default boolean isTeamBoost() {
        return getTeamId() != null;
    }

    @Nullable
    Integer getTeamId();

    Mono<? extends CoreTeam> getTeam();

    void toTeam(int teamId);

    void clear();

    void extend();

    default boolean isNotUsed() {
        return !isTeamBoost();
    }

}
