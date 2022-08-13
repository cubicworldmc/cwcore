package space.cubicworld.core.command.team;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.database.CorePlayerTeamRelation;
import space.cubicworld.core.database.CoreTeam;
import space.cubicworld.core.event.TeamInviteAcceptEvent;
import space.cubicworld.core.message.CoreMessage;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@CoreCommandAnnotation(
        name = "accept",
        permission = "cwcore.team.accept",
        aliases = "a"
)
@RequiredArgsConstructor
public class TeamAcceptCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

    private final VelocityPlugin plugin;

    @Override
    public void execute(VelocityCoreCommandSource source, Iterator<String> args) {
        if (!args.hasNext()) {
            source.sendMessage(CoreMessage.provideTeamName());
            return;
        }
        String teamName = args.next();
        if (!(source.getSource() instanceof Player player)) {
            source.sendMessage(CoreMessage.forPlayer());
            return;
        }
        CoreTeam team = plugin
                .getDatabase()
                .fetchOptionalTeamByName(teamName)
                .orElse(null);
        if (team == null) {
            source.sendMessage(CoreMessage.teamNotExist(teamName));
            return;
        }
        CorePlayerTeamRelation relation = plugin
                .getDatabase()
                .fetchRelation(player.getUniqueId(), team.getId());
        if (relation.getRelation() != CorePlayerTeamRelation.Relation.INVITE) {
            source.sendMessage(CoreMessage.notInvited(team));
            return;
        }
        relation.setRelation(CorePlayerTeamRelation.Relation.MEMBERSHIP);
        relation.update();
        plugin.getServer().getEventManager().fireAndForget(
                TeamInviteAcceptEvent
                        .builder()
                        .team(team)
                        .invited(player)
                        .build()
        );
        source.sendMessage(CoreMessage.inviteAccepted(team));
    }

    @Override
    public List<String> tab(VelocityCoreCommandSource source, Iterator<String> args) {
        if (!args.hasNext()) {
            return Collections.emptyList();
        }
        args.next();
        if (!args.hasNext()) {
            return Collections.singletonList("<team_name>");
        }
        return Collections.emptyList();
    }
}
