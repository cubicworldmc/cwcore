package space.cubicworld.core.command.team;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.database.CoreTeam;
import space.cubicworld.core.event.TeamSelectEvent;
import space.cubicworld.core.message.CoreMessage;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@CoreCommandAnnotation(
        name = "select",
        permission = "cwcore.team.select"
)
@RequiredArgsConstructor
public class TeamSelectCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

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
        CorePlayer corePlayer = plugin.getDatabase()
                .fetchPlayer(player.getUniqueId())
                .orElseThrow();
        plugin.getDatabase()
                .fetchTeam(teamName)
                .ifPresentOrElse(
                        team -> {
                            CoreTeam previous = corePlayer.getSelectedTeam();
                            corePlayer.setSelectedTeam(team);
                            plugin.getDatabase().update(corePlayer);
                            plugin.getServer().getEventManager().fireAndForget(
                                    new TeamSelectEvent(corePlayer, previous, team)
                            );
                            source.sendMessage(CoreMessage.selectTeamSuccess(team));
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
