package space.cubicworld.core.command.team;

import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.CoreCommandNode;
import space.cubicworld.core.command.VelocityCoreCommandSource;

@CoreCommandAnnotation(
        name = "team",
        permission = "cwcore.team",
        aliases = "clan"
)
public class TeamCommand extends CoreCommandNode<VelocityCoreCommandSource> {

    public TeamCommand(VelocityPlugin plugin) {
        command(new TeamCreateCommand(plugin));
        command(new TeamInviteCommand(plugin));
        command(new TeamAcceptCommand(plugin));
    }

}
