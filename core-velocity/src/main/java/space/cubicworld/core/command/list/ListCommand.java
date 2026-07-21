package space.cubicworld.core.command.list;

import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.CoreCommandNode;
import space.cubicworld.core.command.VelocityCoreCommandSource;

@CoreCommandAnnotation(
        name = "list",
        permission = "cwcore.list"
)
public class ListCommand extends CoreCommandNode<VelocityCoreCommandSource> {

    public ListCommand(VelocityPlugin plugin) {
        command(new ListRelationCommand(plugin));
    }

}
