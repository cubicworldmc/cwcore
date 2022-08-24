package space.cubicworld.core.command.profile;

import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.CoreCommandNode;
import space.cubicworld.core.command.VelocityCoreCommandSource;

@CoreCommandAnnotation(
        name = "profile",
        permission = "cwcore.profile"
)
public class ProfileCommand extends CoreCommandNode<VelocityCoreCommandSource> {

    public ProfileCommand(VelocityPlugin plugin) {
        command(new ProfileAboutCommand(plugin));
    }

}
