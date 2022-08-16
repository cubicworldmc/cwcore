package space.cubicworld.core.database;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface CorePTRelation {

    enum Value {
        MEMBERSHIP,
        INVITE,
        NONE
    }

    @NotNull
    CorePlayer getPlayer();

    @NotNull
    CoreTeam getTeam();

    @NotNull
    UUID getPlayerId();

    int getTeamId();

    @NotNull
    Value getValue();

    void setValue(@NotNull Value value);

}
