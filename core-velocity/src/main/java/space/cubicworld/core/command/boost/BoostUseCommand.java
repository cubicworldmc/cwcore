package space.cubicworld.core.command.boost;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
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
        CoreBoost boost = plugin.getDatabase().fetchBoost(id).orElseThrow();
        if (!boost.getPlayerId().equals(player.getUniqueId())) {
            source.sendMessage(CoreMessage.boostActivateOwningFalse());
            return;
        }
        switch (args.next().toLowerCase(Locale.ROOT)) {
            case "team" -> {
                if (!args.hasNext()) {
                    source.sendMessage(CoreMessage.provideTeamName());
                    return;
                }
                String teamName = args.next();
                plugin.getDatabase().fetchTeam(teamName)
                        .ifPresentOrElse(
                                team -> {
                                    boost.toTeam(team.getId());
                                    plugin.getDatabase().update(boost);
                                    plugin.getServer().getEventManager().fireAndForget(
                                            new BoostUpdateEvent(boost)
                                    );
                                    source.sendMessage(CoreMessage.boostAbout(boost));
                                },
                                () -> source.sendMessage(CoreMessage.teamNotExist(teamName))
                        );
            }
            case "clear" -> {
                boost.clear();
                plugin.getDatabase().update(boost);
                plugin.getServer().getEventManager().fireAndForget(new BoostUpdateEvent(boost));
                source.sendMessage(CoreMessage.boostAbout(boost));
            }
            default -> source.sendMessage(CoreMessage.boostNotEdit());
        }
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
