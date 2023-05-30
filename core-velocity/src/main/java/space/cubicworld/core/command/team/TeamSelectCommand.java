package space.cubicworld.core.command.team;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
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
        plugin.getDatabase()
                .fetchPlayer(player.getUniqueId())
                .flatMap(corePlayer -> plugin.getDatabase()
                        .fetchTeam(teamName)
                        .flatMap(team -> {
                            int previous = corePlayer.getSelectedTeamId();
                            corePlayer.setSelectedTeam(team);
                            return plugin.getDatabase().update(corePlayer)
                                    .doOnSuccess(it -> plugin.getServer().getEventManager().fireAndForget(
                                            new TeamSelectEvent(corePlayer, previous, team)
                                    ))
                                    .then(CoreMessage.selectTeamSuccess(team));
                        }).map(it -> (Component) it)
                        .defaultIfEmpty(CoreMessage.teamNotExist(teamName))
                )
                .doOnNext(source::sendMessage)
                .doOnError(this.errorLog(plugin.getLogger()))
                .subscribe();
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
