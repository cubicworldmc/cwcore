package space.cubicworld.core.command.boost;

import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.CoreCommandNode;
import space.cubicworld.core.command.VelocityCoreCommandSource;

@CoreCommandAnnotation(
        name = "boost",
        permission = "cwcore.boost"
)
public class BoostCommand extends CoreCommandNode<VelocityCoreCommandSource> {

    public BoostCommand(VelocityPlugin plugin) {
        super(new BoostInfoCommand(plugin), true);
        command(new BoostAddCommand(plugin));
        command(new BoostActivateCommand(plugin));
        command(new BoostUseCommand(plugin));
    }

}
