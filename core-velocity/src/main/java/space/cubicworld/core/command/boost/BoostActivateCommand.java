package space.cubicworld.core.command.boost;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.database.CoreBoost;
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.message.CoreMessage;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@CoreCommandAnnotation(
        name = "activate",
        permission = "cwcore.boost.activate"
)
@RequiredArgsConstructor
public class BoostActivateCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

    private final VelocityPlugin plugin;

    @Override
    public void execute(VelocityCoreCommandSource source, Iterator<String> args) {
        if (!(source.getSource() instanceof Player player)) {
            source.sendMessage(CoreMessage.forPlayer());
            return;
        }
        CorePlayer corePlayer = plugin.getDatabase()
                .fetchPlayer(player.getUniqueId())
                .orElseThrow();
        if (corePlayer.getInactiveBoosts() <= 0) {
            source.sendMessage(CoreMessage.boostActivateNoBoosts());
            return;
        }
        if (!args.hasNext()) {
            source.sendMessage(CoreMessage.boostActivateConfirm("/boost activate confirm"));
            return;
        }
        String next = args.next();
        if (next.equals("confirm")) {
            plugin.getDatabase().newBoost(corePlayer.getId());
        }
        else if (!args.hasNext() || !args.next().equals("confirm")) {
            source.sendMessage(CoreMessage.boostActivateConfirm("/boost activate " + next + " confirm"));
            return;
        }
        else {
            long id = Long.parseLong(next);
            CoreBoost boost = plugin.getDatabase().fetchBoost(id).orElseThrow();
            if (!boost.getPlayerId().equals(player.getUniqueId())) {
                source.sendMessage(CoreMessage.boostActivateOwningFalse());
                return;
            }
            boost.extend();
            plugin.getDatabase().update(boost);
        }
        corePlayer.decrementInactiveBoosts();
        plugin.getDatabase().update(corePlayer);
        source.sendMessage(CoreMessage.boostMenu(corePlayer, 0));
    }

    @Override
    public List<String> tab(VelocityCoreCommandSource source, Iterator<String> args) {
        return Collections.emptyList();
    }
}
