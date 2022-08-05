package space.cubicworld.core.command.admin;

import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.CoreCommandNode;
import space.cubicworld.core.command.VelocityCoreCommandSource;

@CoreCommandAnnotation(
        name = "cwcore",
        permission = "cwcore.admin"
        /* Not admin command because all sub commands are admin */
)
public class AdminCommand extends CoreCommandNode<VelocityCoreCommandSource> {

    public AdminCommand(VelocityPlugin plugin) {
        command(new AdminReputationChangeCommand(plugin));
    }

}
