package space.cubicworld.core.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.model.CorePlayer;

@RequiredArgsConstructor
public class VelocityJoinListener {

    private final VelocityPlugin plugin;

    @Subscribe
    public void join(PlayerChooseInitialServerEvent event) {
        EntityManager entityManager = plugin.currentSession();
        plugin.beginTransaction();
        if (entityManager.find(CorePlayer.class, event.getPlayer().getUniqueId()) == null) {
            entityManager.persist(CorePlayer
                    .builder()
                    .uuid(event.getPlayer().getUniqueId())
                    .name(event.getPlayer().getUsername())
                    .globalColor(-1)
                    .build()
            );
            plugin.commitTransaction();
        }
        // We do not need to commit transaction if no persist changes were made.
    }

}
