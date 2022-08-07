package space.cubicworld.core.command.admin;

import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.message.CoreMessage;
import space.cubicworld.core.model.CorePlayer;

import java.util.*;

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
            source.sendMessage(CoreMessage.providePlayerName());
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
        Optional<CorePlayer> optionalPlayer = plugin.getPlayerByName().getOptionalModel(playerName);
        if (optionalPlayer.isEmpty()) {
            source.sendMessage(CoreMessage.playerNotExist(playerName));
            return;
        }
        CorePlayer player = optionalPlayer.get();
        int newReputation = player.getReputation();
        switch (operation.toLowerCase(Locale.ROOT)) {
            case "+", "add" -> newReputation += amount;
            case "-", "sub" -> newReputation -= amount;
            case "=", "set" -> newReputation = amount;
        }
        if (newReputation != player.getReputation()) {
            player.setReputation(newReputation);
            plugin.beginTransaction();
            plugin.currentSession().merge(player);
            plugin.commitTransaction();
        }
        source.sendMessage(CoreMessage.playerReputation(player));
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
        args.next();
        if (!args.hasNext()) {
            return List.of("-", "sub", "+", "add", "=", "set");
        }
        return Collections.emptyList();
    }
}


