package space.cubicworld.core.command.team;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.database.CorePlayerTeamRelation;
import space.cubicworld.core.message.CoreMessage;
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.database.CoreTeam;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@CoreCommandAnnotation(
        name = "leave",
        permission = "cwcore.team.leave"
)
@RequiredArgsConstructor
public class TeamLeaveCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

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
        CorePlayer corePlayer = plugin
                .getDatabase()
                .fetchPlayerByUuid(player.getUniqueId());
        CoreTeam team = plugin
                .getDatabase()
                .fetchOptionalTeamByName(teamName)
                .orElse(null);
        if (team == null || team.getOwner().getUuid().equals(corePlayer.getUuid())) {
            source.sendMessage(CoreMessage.teamLeaveCanNot(teamName));
            return;
        }
        CorePlayerTeamRelation relation = plugin
                .getDatabase()
                .fetchRelation(corePlayer.getUuid(), team.getId());
        if (relation.getRelation() != CorePlayerTeamRelation.Relation.MEMBERSHIP) {
            source.sendMessage(CoreMessage.teamLeaveCanNot(teamName));
            return;
        }
        relation.setRelation(CorePlayerTeamRelation.Relation.NONE);
        source.sendMessage(CoreMessage.teamLeaved(team));
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
