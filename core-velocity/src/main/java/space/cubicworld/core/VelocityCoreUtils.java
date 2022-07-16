package space.cubicworld.core;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class VelocityCoreUtils {

    private final VelocityPlugin plugin;

    public boolean isIgnored(Player player) {
        return player.getCurrentServer()
                .map(server -> plugin
                        .getConfig()
                        .getProperty(VelocityConfig.IGNORED_SERVERS)
                        .contains(server.getServer().getServerInfo().getName())
                )
                .orElse(true);
    }

}
