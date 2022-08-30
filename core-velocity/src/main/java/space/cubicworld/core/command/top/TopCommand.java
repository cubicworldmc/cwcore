package space.cubicworld.core.command.top;

import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.CoreCommandNode;
import space.cubicworld.core.command.VelocityCoreCommandSource;

@CoreCommandAnnotation(
        name = "top",
        permission = "cwcore.top"
)
public class TopCommand extends CoreCommandNode<VelocityCoreCommandSource> {

    public TopCommand(VelocityPlugin plugin) {
        command(new TeamsTopCommand(plugin));
        command(new PlayersTopCommand(plugin));
    }

}
