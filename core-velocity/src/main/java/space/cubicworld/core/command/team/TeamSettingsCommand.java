package space.cubicworld.core.command.team;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import reactor.core.publisher.Mono;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.message.CoreMessage;
import space.cubicworld.core.util.ImmutablePair;
import space.cubicworld.core.util.MessageUtils;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

@CoreCommandAnnotation(
        name = "settings",
        permission = "cwcore.team.settings"
)
@RequiredArgsConstructor
public class TeamSettingsCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

    private final VelocityPlugin plugin;

    @Override
    public void execute(VelocityCoreCommandSource source, Iterator<String> args) {
        if (!args.hasNext()) {
            source.sendMessage(CoreMessage.provideTeamName());
            return;
        }
        String teamName = args.next();
        if (!args.hasNext()) {
            source.sendMessage(CoreMessage.teamProvideSettingsValueType());
            return;
        }
        plugin.getDatabase()
                .fetchTeam(teamName)
                .flatMap(team -> switch (args.next().toLowerCase(Locale.ROOT)) {
                    case "description" -> {
                        team.setDescription(MessageUtils.buildMessage(args));
                        yield plugin.getDatabase().update(team).then(CoreMessage.teamSettingsDescriptionUpdated(team));
                    }
                    case "hide" -> {
                        String nextArg = args.hasNext() ?
                                args.next().toLowerCase(Locale.ROOT) : null;
                        if (nextArg == null || (!nextArg.equals("true") && !nextArg.equals("false"))) {
                            yield Mono.just(CoreMessage.teamSettingsHideBad());
                        }
                        boolean value = nextArg.equals("true");
                        team.setHide(value);
                        yield plugin.getDatabase().update(team).then(CoreMessage.teamSettingsHideUpdated(team, value));
                    }
                    case "prefix" -> {
                        if (!team.isVerified()) {
                            yield Mono.just(CoreMessage.teamSettingsPrefixNotVerified());
                        }
                        String nextArg = args.hasNext() ?
                                args.next().toLowerCase(Locale.ROOT) : null;
                        if (nextArg == null || nextArg.length() > 6) {
                            yield Mono.just(CoreMessage.teamSettingsPrefixBad());
                        }
                        yield plugin.getDatabase()
                                .fetchTeamByPrefix(nextArg)
                                .hasElement()
                                .flatMap(exists -> {
                                    if (exists) return Mono.just(CoreMessage.teamSettingsPrefixExist());
                                    team.setPrefix(nextArg);
                                    return plugin.getDatabase().update(team)
                                            .then(CoreMessage.teamSettingsPrefixUpdated(team, nextArg));
                                });
                    }
                    default -> Mono.just(CoreMessage.teamProvideSettingsValueType());
                })
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
        args.next();
        if (!args.hasNext()) {
            return List.of("description", "hide", "prefix");
        }
        if (args.next().equalsIgnoreCase("hide")) {
            return List.of("true", "false");
        }
        return Collections.emptyList();
    }
}
