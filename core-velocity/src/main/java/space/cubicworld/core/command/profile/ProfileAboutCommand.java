package space.cubicworld.core.command.profile;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.message.CoreMessage;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@CoreCommandAnnotation(
        name = "about",
        permission = "cwcore.profile.about"
)
@RequiredArgsConstructor
public class ProfileAboutCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

    private final VelocityPlugin plugin;

    @Override
    public void execute(VelocityCoreCommandSource source, Iterator<String> args) {
        String playerName;
        if (args.hasNext()) {
            playerName = args.next();
        }
        else if (source.getSource() instanceof Player player) {
            playerName = player.getUsername();
        }
        else {
            source.sendMessage(CoreMessage.providePlayerName());
            return;
        }
        plugin.getDatabase()
                .fetchPlayer(playerName)
                .flatMap(CoreMessage::profile).map(it -> (Component) it)
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
        return Collections.emptyList();
    }
}
