package space.cubicworld.core.command.team;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.database.CorePTRelation;
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.event.TeamLeaveEvent;
import space.cubicworld.core.message.CoreMessage;

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
                .fetchPlayer(player.getUniqueId())
                .orElseThrow();
        plugin
                .getDatabase()
                .fetchTeam(teamName)
                .filter(team -> !team.getOwnerId().equals(corePlayer.getId()))
                .ifPresentOrElse(
                        team -> {
                            CorePTRelation relation = plugin
                                    .getDatabase()
                                    .fetchPTRelation(corePlayer.getId(), team.getId())
                                    .orElseThrow();
                            if (relation.getValue() != CorePTRelation.Value.MEMBERSHIP) {
                                source.sendMessage(CoreMessage.teamLeaveCanNot(teamName));
                            }
                            relation.setValue(CorePTRelation.Value.NONE);
                            plugin.getDatabase().update(relation);
                            source.sendMessage(CoreMessage.teamLeaved(team));
                            plugin.getServer().getEventManager()
                                    .fireAndForget(new TeamLeaveEvent(corePlayer, team));
                        },
                        () -> source.sendMessage(CoreMessage.teamLeaveCanNot(teamName))
                );
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
