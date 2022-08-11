package space.cubicworld.core.command.team;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.message.CoreMessage;
import space.cubicworld.core.model.CorePlayer;
import space.cubicworld.core.model.CorePlayerTeamLink;
import space.cubicworld.core.model.CoreTeam;
import space.cubicworld.core.model.CoreTeamMember;

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
        plugin.beginTransaction();
        CorePlayer corePlayer = plugin.currentSession().find(CorePlayer.class, player.getUniqueId());
        CoreTeam team = plugin
                .currentSession()
                .createQuery(
                        """
                                FROM CoreTeam team
                                INNER JOIN CoreTeamMember member ON member.link.player = :player
                                WHERE NOT team.owner = :player AND team.name = :name
                                """,
                        CoreTeam.class
                )
                .setParameter("player", corePlayer)
                .setParameter("name", teamName)
                .getSingleResultOrNull();
        if (team == null) {
            source.sendMessage(CoreMessage.teamLeaveCanNot(teamName));
            return;
        }
        plugin.currentSession().remove(new CoreTeamMember(
                new CorePlayerTeamLink(corePlayer, team))
        );
        plugin.commitTransaction();
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
