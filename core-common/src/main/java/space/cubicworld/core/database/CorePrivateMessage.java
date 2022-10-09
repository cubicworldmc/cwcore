package space.cubicworld.core.database;

import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.sql.Time;

public interface CorePrivateMessage {

    long getId();

    @NotNull
    Mono<CorePlayer> getSender();

    @NotNull
    Mono<CorePlayer> getReceiver();

    @NotNull
    String getMessage();

    @NotNull
    Time getSentTime();

}
