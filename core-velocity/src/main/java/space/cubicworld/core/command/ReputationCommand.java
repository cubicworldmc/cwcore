package space.cubicworld.core.command;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.hibernate.Transaction;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.model.CorePlayer;

import java.util.Iterator;
import java.util.List;

@CoreCommandAnnotation(
        name = "reputation",
        aliases = "rep",
        permission = "cwcore.reputation.see"
)
@RequiredArgsConstructor
public class ReputationCommand implements CoreCommand<VelocityCoreCommandSource> {

    private final VelocityPlugin plugin;

    @Override
    public void execute(VelocityCoreCommandSource source, Iterator<String> args) {
        String name;
        if (args.hasNext()) {
            name = args.next();
        } else if (source.getSource() instanceof Player player) {
            name = player.getUsername();
        } else {
            source.sendMessage(Component
                    .text("Player nickname should be passed")
                    .color(NamedTextColor.RED)
            );
            return;
        }
        plugin.currentTransaction();
        int playerReputation = plugin
                .currentSession()
                .createQuery(
                        "SELECT p.reputation FROM CorePlayer p WHERE p.name = :name",
                        Integer.class
                )
                .setParameter("name", name)
                .getSingleResultOrNull();
        source.sendMessage(Component
                .text("Reputation of the player %s is %s".formatted(
                        name,
                        Integer.toString(playerReputation)
                ))
        );
    }

    @Override
    public List<String> tab(VelocityCoreCommandSource source, Iterator<String> args) {
        return null;
    }
}
