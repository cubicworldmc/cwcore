package space.cubicworld.core.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.VelocityUtils;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ReputationCommand implements SimpleCommand {

    private final VelocityPlugin plugin;

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 0) {
            VelocityUtils.sendChat(
                    invocation.source(),
                    plugin.getMessageContainer().providePlayer(),
                    plugin.getMessageContainer()
            );
            return;
        }
        String nickname = args[0];
        try {
            plugin.getPlayerNameReference().get(nickname)
                    .ifPresentOrElse(
                            player -> VelocityUtils.sendChat(
                                    invocation.source(),
                                    plugin.getMessageContainer().seeReputation(
                                            plugin.getMessageContainer().playerMarked(player),
                                            player.getReputation()
                                    ),
                                    plugin.getMessageContainer()
                            ),
                            () -> VelocityUtils.sendChat(
                                    invocation.source(),
                                    plugin.getMessageContainer().unknownPlayer(nickname),
                                    plugin.getMessageContainer()
                            )
                    );
        } catch (SQLException e) {
            plugin.getLogger().warn("Failed to fetch player with name {}:", nickname, e);
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().getPermissionValue("cwcore.rep.see") != Tristate.FALSE;
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        String start = "";
        if (args.length != 0) start = args[0];
        String finalStart = start;
        return plugin
                .getServer()
                .getAllPlayers()
                .stream()
                .map(Player::getUsername)
                .filter(username -> username.startsWith(finalStart))
                .limit(32)
                .collect(Collectors.toList());
    }
}
