package space.cubicworld.core.command.color;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.TriState;
import reactor.core.publisher.Mono;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.color.CoreColor;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.event.ColorChangeEvent;
import space.cubicworld.core.message.CoreMessage;
import space.cubicworld.core.util.ColorUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@CoreCommandAnnotation(
        name = "color",
        permission = "cwcore.color"
)
@RequiredArgsConstructor
public class ColorCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

    private final VelocityPlugin plugin;

    @Override
    public void execute(VelocityCoreCommandSource source, Iterator<String> args) {
        if (!(source.getSource() instanceof Player player)) {
            source.sendMessage(CoreMessage.forPlayer());
            return;
        }
        plugin.getDatabase()
                .fetchPlayer(player.getUniqueId())
                .flatMap(corePlayer -> {
                    if (!args.hasNext()) {
                        source.sendMessage(CoreMessage.colorInfo(
                                source.getPermission("cwcore.color.custom") == TriState.TRUE,
                                corePlayer,
                                plugin.getCore().getColorIndexContainer().getColors()
                        ));
                        return Mono.empty();
                    }
                    String color = args.next();
                    TextColor textColor;
                    CoreColor coreColor;
                    if (color.equals("-")) { // by index
                        if (!args.hasNext()) return Mono.empty();
                        int index = Integer.parseInt(args.next());
                        textColor = plugin.getCore()
                                .getColorIndexContainer()
                                .getColor(index, corePlayer)
                                .orElse(null);
                        coreColor = CoreColor.fromIndex(index);
                    } else {
                        textColor = ColorUtils.checkedFromLocalized(color);
                        coreColor = CoreColor.fromCustom(ColorUtils.checkedFromLocalized(color));
                    }
                    if (textColor == null) {
                        source.sendMessage(CoreMessage.colorBad());
                        return Mono.empty();
                    }
                    CoreColor previous = corePlayer.getGlobalColor();
                    corePlayer.setGlobalColor(coreColor);
                    return plugin.getDatabase().update(corePlayer)
                            .doOnSuccess(it -> {
                                source.sendMessage(CoreMessage.colorSuccess(textColor));
                                plugin.getServer().getEventManager().fireAndForget(
                                        new ColorChangeEvent(corePlayer, previous, coreColor)
                                );
                            });
                })
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
            return new ArrayList<>(NamedTextColor.NAMES.keys());
        }
        return Collections.emptyList();
    }
}
