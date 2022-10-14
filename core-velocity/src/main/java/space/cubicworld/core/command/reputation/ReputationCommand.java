package space.cubicworld.core.command.reputation;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import reactor.core.publisher.Mono;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.message.CoreMessage;

import java.util.Iterator;
import java.util.List;

@CoreCommandAnnotation(
        name = "reputation",
        aliases = "rep",
        permission = "cwcore.reputation.see"
)
@RequiredArgsConstructor
public class ReputationCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

    private final VelocityPlugin plugin;

    @Override
    public void execute(VelocityCoreCommandSource source, Iterator<String> args) {
        String name;
        if (args.hasNext()) {
            name = args.next();
        } else if (source.getSource() instanceof Player player) {
            name = player.getUsername();
        } else {
            source.sendMessage(CoreMessage.providePlayerName());
            return;
        }
        plugin.getDatabase().fetchPlayer(name)
                .flatMap(CoreMessage::playerReputation).map(it -> (Component) it)
                .switchIfEmpty(Mono.defer(() -> Mono.just(CoreMessage.playerNotExist(name))))
                .doOnNext(source::sendMessage)
                .doOnError(this.errorLog(plugin.getLogger()))
                .subscribe();
    }

    @Override
    public List<String> tab(VelocityCoreCommandSource source, Iterator<String> args) {
        return plugin.commandHelper().playersTab();
    }
}
