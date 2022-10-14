package space.cubicworld.core.command.team;

import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import reactor.core.publisher.Mono;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.database.CorePTRelation;
import space.cubicworld.core.message.CoreMessage;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@CoreCommandAnnotation(
        name = "about",
        permission = "cwcore.team.about"
)
@RequiredArgsConstructor
public class TeamAboutCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

    private final VelocityPlugin plugin;

    @Override
    public void execute(VelocityCoreCommandSource source, Iterator<String> args) {
        if (!args.hasNext()) {
            source.sendMessage(CoreMessage.provideTeamName());
            return;
        }
        String teamName = args.next();
        plugin.getDatabase()
                .fetchTeam(teamName)
                .flatMap(team -> Mono.just(source.getSource().getPermissionValue("cwcore.team.about.hide.ignore") == Tristate.TRUE)
                        .flatMap(value -> value ? Mono.just(true) : (
                                        source.getSource() instanceof Player player ?
                                                plugin.getDatabase().fetchPTRelation(player.getUniqueId(), team.getId())
                                                        .map(relation -> relation.getValue() == CorePTRelation.Value.MEMBERSHIP) :
                                                Mono.just(false)
                                )
                        )
                        .flatMap(forMember -> CoreMessage.teamAbout(team, forMember))
                ).map(it -> (Component) it)
                .defaultIfEmpty(CoreMessage.teamNotExist(teamName))
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
