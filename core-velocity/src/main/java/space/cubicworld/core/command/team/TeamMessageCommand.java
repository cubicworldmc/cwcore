package space.cubicworld.core.command.team;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.database.CorePTRelation;
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.event.TeamMessageEvent;
import space.cubicworld.core.message.CoreMessage;
import space.cubicworld.core.util.MessageUtils;

import java.sql.ResultSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@CoreCommandAnnotation(
        name = "message",
        permission = "cwcore.team.message",
        aliases = {"msg", "tell", "w"}
)
@RequiredArgsConstructor
public class TeamMessageCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

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
                .fetchTeam(teamName)
                .doOnSuccess(team -> {
                    if (team == null) source.sendMessage(CoreMessage.teamNotExist(teamName));
                })
                .flatMap(team -> plugin
                        .getDatabase()
                        .fetchPTRelation(player.getUniqueId(), team.getId()).flatMap(relation -> {
                            if (relation.getValue() != CorePTRelation.Value.MEMBERSHIP) {
                                return CoreMessage.teamNotMemberSelf(team);
                            }
                            String message = MessageUtils.buildMessage(args);
                            if (message == null) {
                                source.sendMessage(CoreMessage.teamMessageEmpty());
                                return Mono.empty();
                            }
                            return plugin.getDatabase()
                                    .fetchPlayer(player.getUniqueId())
                                    .doOnNext(corePlayer -> plugin.getServer().getEventManager().fireAndForget(
                                            new TeamMessageEvent(corePlayer, team, message)
                                    ));
                        })
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
