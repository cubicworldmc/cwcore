package space.cubicworld.core.database;

import org.jetbrains.annotations.NotNull;

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
