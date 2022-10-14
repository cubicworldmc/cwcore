package space.cubicworld.core.command.boost;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import reactor.core.publisher.Mono;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.database.CoreBoost;
import space.cubicworld.core.event.BoostUpdateEvent;
import space.cubicworld.core.message.CoreMessage;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

@CoreCommandAnnotation(
        name = "use",
        permission = "cwcore.boost.use"
)
@RequiredArgsConstructor
public class BoostUseCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

    private final VelocityPlugin plugin;

    @Override
    public void execute(VelocityCoreCommandSource source, Iterator<String> args) {
        if (!(source.getSource() instanceof Player player)) {
            source.sendMessage(CoreMessage.forPlayer());
            return;
        }
        if (!args.hasNext()) {
            source.sendMessage(CoreMessage.boostNotEdit());
            return;
        }
        Long id = Long.parseLong(args.next());
        if (!args.hasNext()) {
            source.sendMessage(CoreMessage.boostNotEdit());
            return;
        }
        plugin.getDatabase()
                .fetchBoost(id)
                .flatMap(boost -> {
                    if (!boost.getPlayerId().equals(player.getUniqueId())) {
                        source.sendMessage(CoreMessage.boostActivateOwningFalse());
                        return Mono.empty();
                    }
                    return switch (args.next().toLowerCase(Locale.ROOT)) {
                        case "team" -> {
                            if (!args.hasNext()) {
                                yield Mono.just(CoreMessage.provideTeamName());
                            }
                            String teamName = args.next();
                            yield plugin.getDatabase().fetchTeam(teamName)
                                    .flatMap(team -> {
                                        boost.toTeam(team.getId());
                                        return plugin.getDatabase().update(boost)
                                                .doOnSuccess(it -> plugin.getServer().getEventManager().fireAndForget(
                                                        new BoostUpdateEvent(boost)
                                                ))
                                                .then(CoreMessage.boostAbout(boost));
                                    }).map(it -> (Component) it)
                                    .defaultIfEmpty(CoreMessage.teamNotExist(teamName));
                        }
                        case "clear" -> {
                            boost.clear();
                            yield plugin.getDatabase().update(boost)
                                    .doOnSuccess(it -> plugin.getServer().getEventManager().fireAndForget(new BoostUpdateEvent(boost)))
                                    .then(CoreMessage.boostAbout(boost));
                        }
                        default -> Mono.just(CoreMessage.boostNotEdit());
                    };
                })
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
            return Collections.singletonList("<boost_id>");
        }
        args.next();
        if (!args.hasNext()) {
            return List.of("team");
        }
        return switch (args.next().toLowerCase(Locale.ROOT)) {
            case "team" -> Collections.singletonList("<team_name>");
            default -> Collections.emptyList();
        };
    }
}
