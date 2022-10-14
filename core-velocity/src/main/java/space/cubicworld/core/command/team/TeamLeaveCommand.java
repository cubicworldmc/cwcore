package space.cubicworld.core.command.team;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import reactor.core.publisher.Mono;
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
        plugin
                .getDatabase()
                .fetchPlayer(player.getUniqueId())
                .flatMap(corePlayer -> plugin
                        .getDatabase()
                        .fetchTeam(teamName)
                        .filter(team -> !team.getOwnerId().equals(corePlayer.getId()))
                        .flatMap(team -> plugin
                                .getDatabase()
                                .fetchPTRelation(corePlayer.getId(), team.getId())
                                .flatMap(relation -> {
                                    if (relation.getValue() != CorePTRelation.Value.MEMBERSHIP) {
                                        return Mono.just(CoreMessage.teamLeaveCanNot(teamName));
                                    }
                                    relation.setValue(CorePTRelation.Value.NONE);
                                    return plugin.getDatabase().update(relation)
                                            .then(CoreMessage.teamLeaved(team))
                                            .doOnNext(it -> plugin.getServer().getEventManager()
                                                    .fireAndForget(new TeamLeaveEvent(corePlayer, team))
                                            );
                                })
                        )
                        .defaultIfEmpty(CoreMessage.teamLeaveCanNot(teamName))
                        .doOnNext(source::sendMessage)
                )
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
