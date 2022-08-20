package space.cubicworld.core.command.boost;

import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.event.BoostAddedEvent;
import space.cubicworld.core.message.CoreMessage;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@CoreCommandAnnotation(
        name = "add",
        permission = "cwcore.boost.add",
        admin = true
)
@RequiredArgsConstructor
public class BoostAddCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

    private final VelocityPlugin plugin;

    @Override
    public void execute(VelocityCoreCommandSource source, Iterator<String> args) {
        if (!args.hasNext()) {
            source.sendMessage(CoreMessage.providePlayerName());
            return;
        }
        String playerName = args.next();
        plugin.getDatabase().fetchPlayer(playerName)
                .ifPresentOrElse(
                        player -> {
                            player.incrementInactiveBoosts();
                            plugin.getDatabase().update(player);
                            plugin.getServer().getEventManager().fireAndForget(
                                    new BoostAddedEvent(player)
                            );
                            source.sendMessage(CoreMessage.addedOneBoost(player));
                        },
                        () -> source.sendMessage(CoreMessage.playerNotExist(playerName))
                );
    }

    @Override
    public List<String> tab(VelocityCoreCommandSource source, Iterator<String> args) {
        if (!args.hasNext()) {
            return Collections.emptyList();
        }
        args.next();
        if (!args.hasNext()) {
            return plugin.commandHelper().playersTab();
        }
        return Collections.emptyList();
    }
}
