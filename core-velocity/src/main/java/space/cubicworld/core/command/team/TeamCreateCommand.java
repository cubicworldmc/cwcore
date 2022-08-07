package space.cubicworld.core.command.team;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.event.TeamCreateEvent;
import space.cubicworld.core.message.CoreMessage;
import space.cubicworld.core.model.CorePlayer;
import space.cubicworld.core.model.CoreTeam;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@CoreCommandAnnotation(
        name = "create",
        permission = "cwcore.team.create"
)
@RequiredArgsConstructor
public class TeamCreateCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

    private final VelocityPlugin plugin;

    @Override
    public void execute(VelocityCoreCommandSource source, Iterator<String> args) {
        if (!args.hasNext()) {
            source.sendMessage(CoreMessage.provideTeamName());
            return;
        }
        String teamName = args.next();
        if (!(source.getSource() instanceof Player player)) {
            source.sendMessage(CoreMessage.providePlayerName());
            return;
        }
        plugin.beginTransaction();
        if (plugin.getTeamByName().getOptionalModel(teamName).isEmpty()) {
            CoreTeam team = CoreTeam
                    .builder()
                    .name(teamName)
                    .owner(CorePlayer
                            .builder()
                            .uuid(player.getUniqueId())
                            .build()
                    )
                    .build();
            plugin.currentSession().persist(team);
            plugin.commitTransaction();
            plugin.getServer().getEventManager().fireAndForget(
                    TeamCreateEvent
                            .builder()
                            .owner(player)
                            .team(team)
                            .build()
            );
            source.sendMessage(CoreMessage.teamCreated(team));
        } else {
            source.sendMessage(CoreMessage.teamAlreadyExist(teamName));
        }
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
