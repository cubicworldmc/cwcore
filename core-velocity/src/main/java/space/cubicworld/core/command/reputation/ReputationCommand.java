package space.cubicworld.core.command.reputation;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.message.CoreMessage;
import space.cubicworld.core.database.CorePlayer;

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
        CorePlayer player = plugin
                .getDatabase()
                .fetchOptionalPlayerByName(name)
                .orElse(null);
        source.sendMessage(player == null ?
                CoreMessage.playerNotExist(name) :
                CoreMessage.playerReputation(player)
        );
    }

    @Override
    public List<String> tab(VelocityCoreCommandSource source, Iterator<String> args) {
        return plugin.commandHelper().playersTab();
    }
}
