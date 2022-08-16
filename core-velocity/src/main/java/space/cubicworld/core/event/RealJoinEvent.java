package space.cubicworld.core.event;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RealJoinEvent {

    private final Player player;
    private final RegisteredServer server;

}
