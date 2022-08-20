package space.cubicworld.core.command.team;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.database.CorePTRelation;
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
        plugin.getDatabase()
                .fetchTeam(teamName)
                .ifPresentOrElse(
                        team -> {
                            if (team.getMaxMembers() < team.getRelationsCount(CorePTRelation.Value.MEMBERSHIP)) {
                                source.sendMessage(CoreMessage.playersLimitIncreased(team));
                                return;
                            }
                            CorePTRelation relation = plugin.getDatabase()
                                    .fetchPTRelation(player.getUniqueId(), team.getId())
                                    .orElseThrow();
                            if (!relation.getValue().isInvite()) {
                                source.sendMessage(CoreMessage.notInvited(team));
                                return;
                            }
                            relation.setValue(CorePTRelation.Value.MEMBERSHIP);
                            plugin.getDatabase().update(relation);
                            plugin.getServer().getEventManager().fireAndForget(
                                    new TeamInviteAcceptEvent(player, team)
                            );
                            source.sendMessage(CoreMessage.inviteAccepted(team));
                        },
                        () -> source.sendMessage(CoreMessage.teamNotExist(teamName))
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
