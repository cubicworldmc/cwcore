package space.cubicworld.core.command.team;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.message.CoreMessage;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@CoreCommandAnnotation(
        name = "invites",
        permission = "cwcore.team.invites"
)
@RequiredArgsConstructor
public class TeamInvitesCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

    private final VelocityPlugin plugin;

    @Override
    public void execute(VelocityCoreCommandSource source, Iterator<String> args) {
        if (!(source.getSource() instanceof Player player)) {
            source.sendMessage(CoreMessage.forPlayer());
            return;
        }
        int page = args.hasNext() ? Integer.parseInt(args.next()) - 1 : 0;
        CorePlayer corePlayer = plugin.getDatabase()
                .fetchPlayer(player.getUniqueId())
                .orElseThrow();
        source.sendMessage(CoreMessage.teamInvitesPage(corePlayer, page));
    }

    @Override
    public List<String> tab(VelocityCoreCommandSource source, Iterator<String> args) {
        return Collections.emptyList();
    }
}
