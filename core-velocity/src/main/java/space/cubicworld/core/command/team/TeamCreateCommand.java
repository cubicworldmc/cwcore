package space.cubicworld.core.command.team;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import reactor.core.publisher.Mono;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.database.CorePTRelation;
import space.cubicworld.core.event.TeamCreateEvent;
import space.cubicworld.core.message.CoreMessage;

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
    @SneakyThrows
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
                .fetchPlayerNotVerifiedOwnedTeam(player.getUniqueId())
                .flatMap(CoreMessage::oneTeamNotVerified).map(it -> (Component) it)
                .switchIfEmpty(plugin.getDatabase()
                        .fetchTeam(teamName)
                        .hasElement()
                        .flatMap(existsNamedTeam -> existsNamedTeam ?
                                Mono.just(CoreMessage.teamAlreadyExist(teamName)) :
                                plugin.getDatabase()
                                        .newTeam(teamName, player.getUniqueId())
                                        .flatMap(team -> plugin.getDatabase()
                                                .fetchPTRelation(player.getUniqueId(), team.getId())
                                                .doOnNext(relation -> relation.setValue(CorePTRelation.Value.MEMBERSHIP))
                                                .flatMap(relation -> plugin.getDatabase().update(relation)
                                                        .doOnSuccess(it -> plugin.getServer()
                                                                .getEventManager()
                                                                .fireAndForget(new TeamCreateEvent(player, team))
                                                        )
                                                )
                                                .then(CoreMessage.teamCreated(team))
                                        )
                        )
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
