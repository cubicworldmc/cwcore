package space.cubicworld.core.command;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;

import java.util.List;

@RequiredArgsConstructor
public class VelocityCommandHelper {

    private final VelocityPlugin plugin;

    public List<String> playersTab() {
        return plugin
                .getServer()
                .getAllPlayers()
                .stream()
                .limit(50)
                .map(Player::getUsername)
                .toList();
    }

}
