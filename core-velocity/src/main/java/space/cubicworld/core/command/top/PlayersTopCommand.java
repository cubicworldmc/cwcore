package space.cubicworld.core.command.top;

import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.message.CoreMessage;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@CoreCommandAnnotation(
        name = "players",
        permission = "cwcore.top.players"
)
@RequiredArgsConstructor
public class PlayersTopCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

    private final VelocityPlugin plugin;

    @Override
    public void execute(VelocityCoreCommandSource source, Iterator<String> args) {
        int page = args.hasNext() ? Integer.parseInt(args.next()) - 1 : 0;
        plugin.getDatabase().fetchPlayerReputationTop(page)
                .collectList()
                .flatMap(CoreMessage::playersReputationTop)
                .doOnNext(source::sendMessage)
                .doOnError(this.errorLog(plugin.getLogger()))
                .subscribe();
    }

    @Override
    public List<String> tab(VelocityCoreCommandSource source, Iterator<String> args) {
        return Collections.emptyList();
    }
}
