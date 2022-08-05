package space.cubicworld.core.command.admin;

import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.model.CorePlayer;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

@CoreCommandAnnotation(
        name = "reputation",
        permission = "cwcore.reputation.change",
        aliases = "rep",
        admin = true
)
@RequiredArgsConstructor
public class AdminReputationChangeCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

    private final VelocityPlugin plugin;

    @Override
    public void execute(VelocityCoreCommandSource source, Iterator<String> args) {
        if (!args.hasNext()) {
            return;
        }
        String playerName = args.next();
        if (!args.hasNext()) {
            return;
        }
        String operation = args.next();
        if (!args.hasNext()) {
            return;
        }
        int amount = Integer.parseInt(args.next());
        plugin.beginTransaction();
        CorePlayer player = plugin.currentSession()
                .createQuery(
                        "FROM CorePlayer p WHERE p.name = :name",
                        CorePlayer.class
                )
                .setParameter("name", playerName)
                .getSingleResultOrNull();
        if (player == null) {
            return;
        }
        int newReputation = player.getReputation();
        switch (operation.toLowerCase(Locale.ROOT)) {
            case "+", "add" -> newReputation += amount;
            case "-", "sub" -> newReputation -= amount;
            case "=", "set" -> newReputation = amount;
        }
        if (newReputation == player.getReputation()) {
            return;
        }
        player.setReputation(newReputation);
        plugin.currentSession().merge(player);
        plugin.commitTransaction();
    }

    @Override
    public List<String> tab(VelocityCoreCommandSource source, Iterator<String> args) {
        if (!args.hasNext()) {
            return plugin.commandHelper().playersTab();
        }
        args.next();
        if (!args.hasNext()) {
            return List.of("-", "sub", "+", "add", "=", "set");
        }
        return Collections.emptyList();
    }
}


