package space.cubicworld.core.command.admin;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import reactor.core.publisher.Mono;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.event.ReputationChangeEvent;
import space.cubicworld.core.message.CoreMessage;

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
    @SneakyThrows
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
        plugin.getDatabase()
                .fetchPlayer(playerName)
                .flatMap(player -> {
                    int newReputation = player.getReputation();
                    switch (operation.toLowerCase(Locale.ROOT)) {
                        case "+", "add" -> newReputation += amount;
                        case "-", "sub" -> newReputation -= amount;
                        case "=", "set" -> newReputation = amount;
                    }
                    int previous = player.getReputation();
                    Mono<Void> result;
                    if (newReputation != previous) {
                        player.setReputation(newReputation);
                        result = plugin.getDatabase().update(player);
                        plugin.getServer().getEventManager().fireAndForget(
                                new ReputationChangeEvent(player, previous, newReputation)
                        );
                    } else {
                        result = Mono.empty();
                    }
                    return result.then(CoreMessage.playerReputation(player).doOnNext(source::sendMessage));
                }).map(it -> (Component) it)
                .defaultIfEmpty(CoreMessage.playerNotExist(playerName))
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
            return plugin.commandHelper().playersTab();
        }
        args.next();
        if (!args.hasNext()) {
            return List.of("-", "sub", "+", "add", "=", "set");
        }
        return Collections.emptyList();
    }
}


