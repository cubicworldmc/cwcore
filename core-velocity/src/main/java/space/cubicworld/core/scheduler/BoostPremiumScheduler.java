package space.cubicworld.core.scheduler;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.scheduler.ScheduledTask;
import lombok.RequiredArgsConstructor;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.data.TemporaryNodeMergeStrategy;
import net.luckperms.api.node.Node;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.database.CoreBoost;
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.event.BoostActivateEvent;
import space.cubicworld.core.event.RealJoinEvent;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class BoostPremiumScheduler {

    private final VelocityPlugin plugin;
    private final LuckPerms luckPerms = LuckPermsProvider.get();

    @Subscribe
    public void join(RealJoinEvent event) {
        updateTime(event.getPlayer().getUniqueId());
    }

    @Subscribe
    public void boostActivate(BoostActivateEvent event) {
        updateTime(event.getBoost().getPlayerId());
    }

    private void updateTime(UUID uuid) {
        plugin.getCore()
                .getDatabase()
                .fetchPlayer(uuid)
                .flatMap(player -> player.getBoosts()
                        .stream()
                        .max(Comparator.comparingLong(CoreBoost::getEnd))
                )
                .ifPresent(boost -> luckPerms.getUserManager()
                        .modifyUser(uuid, user -> user.data().add(
                                        Node.builder(plugin.getConfig().get("premium-permission"))
                                                .value(true)
                                                .expiry(boost.getEnd() / 1000)
                                                .build(),
                                        TemporaryNodeMergeStrategy.REPLACE_EXISTING_IF_DURATION_LONGER
                                )
                        )
                );
    }

}
