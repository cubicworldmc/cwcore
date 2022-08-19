package space.cubicworld.core.command.team;

import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.message.CoreMessage;

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
                .ifPresentOrElse(
                        team -> {
                            switch (args.next().toLowerCase(Locale.ROOT)) {
                                case "description" -> {
                                    StringBuilder stringBuilder = new StringBuilder();
                                    args.forEachRemaining(str -> stringBuilder.append(str).append(' '));
                                    String description = stringBuilder.isEmpty() ?
                                            null :
                                            stringBuilder.substring(0, stringBuilder.length() - 1);
                                    team.setDescription(description);
                                    source.sendMessage(CoreMessage.teamSettingsDescriptionUpdated(team));
                                }
                                case "hide" -> {
                                    String nextArg = args.hasNext() ?
                                            args.next().toLowerCase(Locale.ROOT) : null;
                                    if (nextArg == null || (!nextArg.equals("true") && !nextArg.equals("false"))) {
                                        source.sendMessage(CoreMessage.teamSettingsHideBad());
                                        return;
                                    }
                                    boolean value = nextArg.equals("true");
                                    team.setHide(value);
                                    source.sendMessage(CoreMessage.teamSettingsHideUpdated(team, value));
                                }
                            }
                            plugin.getDatabase().update(team);
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
        args.next();
        if (!args.hasNext()) {
            return List.of("description", "hide");
        }
        if (args.next().equalsIgnoreCase("hide")) {
            return List.of("true", "false");
        }
        return Collections.emptyList();
    }
}
