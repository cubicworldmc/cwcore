package space.cubicworld.core.command.color;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.format.TextColor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.event.ColorChangeEvent;
import space.cubicworld.core.message.CoreMessage;
import space.cubicworld.core.util.ColorUtils;

import java.util.Iterator;
import java.util.List;

@CoreCommandAnnotation(
        name = "color",
        permission = "cwcore.color"
)
public class ColorCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

    private final VelocityPlugin plugin;
    private final ColorRuleContainer ruleContainer;

    public ColorCommand(VelocityPlugin plugin) {
        this.plugin = plugin;
        ruleContainer = new ColorRuleContainer(this.plugin);
    }

    @Override
    public void execute(VelocityCoreCommandSource source, Iterator<String> args) {
        if (!args.hasNext()) {

            return;
        }
        String color = args.next();
        if (!(source.getSource() instanceof Player player)) {
            source.sendMessage(CoreMessage.forPlayer());
            return;
        }
        CorePlayer corePlayer = plugin.getDatabase()
                .fetchPlayer(player.getUniqueId())
                .orElseThrow();
        TextColor current;
        TextColor previous = corePlayer.getGlobalColor();
        if (color.startsWith("-")) {
            int index = Integer.parseInt(args.next());
            ColorRule rule = ruleContainer.getRule(index);
            if (rule == null || !rule.isMatch(player)) {
                return;
            }
            current = rule.getColor();
        } else {
            current = ColorUtils.checkedFromLocalized(color);
            if (current == null) {
                source.sendMessage(CoreMessage.colorBad());
                return;
            }
        }
        corePlayer.setGlobalColor(current);
        source.sendMessage(CoreMessage.colorSuccess(current));
        plugin.getServer().getEventManager().fireAndForget(
                new ColorChangeEvent(corePlayer, previous, current)
        );
    }

    @Override
    public List<String> tab(VelocityCoreCommandSource source, Iterator<String> args) {
        return null;
    }
}
