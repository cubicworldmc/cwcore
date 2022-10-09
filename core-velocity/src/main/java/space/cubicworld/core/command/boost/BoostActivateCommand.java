package space.cubicworld.core.command.boost;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.database.CoreBoost;
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.event.BoostActivateEvent;
import space.cubicworld.core.message.CoreMessage;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@CoreCommandAnnotation(
        name = "activate",
        permission = "cwcore.boost.activate"
)
@RequiredArgsConstructor
public class BoostActivateCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

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
                    if (corePlayer.getInactiveBoosts() <= 0) {
                        return Mono.just(CoreMessage.boostActivateNoBoosts());
                    }
                    if (!args.hasNext()) {
                        return Mono.just(CoreMessage.boostActivateConfirm("/boost activate confirm"));
                    }
                    String next = args.next();
                    Mono<? extends CoreBoost> boostMono;
                    boolean extend;
                    if (next.equals("confirm")) {
                        boostMono = plugin.getDatabase().newBoost(corePlayer.getId());
                        extend = false;
                    } else if (!args.hasNext() || !args.next().equals("confirm")) {
                        return Mono.just(CoreMessage.boostActivateConfirm("/boost activate " + next + " confirm"));
                    } else {
                        long id = Long.parseLong(next);
                        boostMono = plugin.getDatabase().fetchBoost(id)
                                .flatMap(boost -> {
                                    if (!boost.getPlayerId().equals(player.getUniqueId())) {
                                        source.sendMessage(CoreMessage.boostActivateOwningFalse());
                                        return Mono.empty();
                                    }
                                    boost.extend();
                                    return plugin.getDatabase().update(boost).thenReturn(boost);
                                });
                        extend = true;
                    }
                    corePlayer.setInactiveBoosts(corePlayer.getInactiveBoosts() - 1);
                    return plugin.getDatabase().update(corePlayer)
                            .then(boostMono)
                            .doOnNext(boost -> plugin.getServer().getEventManager().fireAndForget(
                                    new BoostActivateEvent(boost, extend)
                            ))
                            .flatMap(boost -> CoreMessage.boostMenu(corePlayer, 0));
                })
                .doOnNext(source::sendMessage)
                .doOnError(this.errorLog(plugin.getLogger()))
                .subscribe();
    }

    @Override
    public List<String> tab(VelocityCoreCommandSource source, Iterator<String> args) {
        return Collections.emptyList();
    }
}
