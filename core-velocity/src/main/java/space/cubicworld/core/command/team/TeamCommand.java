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
        command(new TeamVerifyCommand(plugin));
        command(new TeamAboutCommand(plugin));
        command(new TeamLeaveCommand(plugin));
        command(new TeamDeleteCommand(plugin));
        command(new TeamMessageCommand(plugin));
        command(new TeamInvitesCommand(plugin));
        command(new TeamKickCommand(plugin));
        command(new TeamSettingsCommand(plugin));
        command(new TeamReadCommand(plugin));
        command(new TeamVerifiesCommand(plugin));
        command(new TeamSelectCommand(plugin));
    }

}
