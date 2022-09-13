package space.cubicworld.core.database;

import org.jetbrains.annotations.NotNull;

import java.sql.Time;

public interface CorePrivateMessage {

    long getId();

    @NotNull
    CorePlayer getSender();

    @NotNull
    CorePlayer getReceiver();

    @NotNull
    String getMessage();

    @NotNull
    Time getSentTime();

}
