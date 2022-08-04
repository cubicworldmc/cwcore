package space.cubicworld.core.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.Transaction;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.model.CorePlayer;

@RequiredArgsConstructor
public class VelocityJoinListener {

    private final VelocityPlugin plugin;

    @Subscribe
    public void join(PlayerChooseInitialServerEvent event) {
        EntityManager entityManager = plugin
                .getCore()
                .getHibernateSessionFactory()
                .getCurrentSession();
        Transaction transaction = plugin.currentTransaction();
        if (entityManager.find(CorePlayer.class, event.getPlayer().getUniqueId()) == null) {
            entityManager.persist(CorePlayer
                    .builder()
                    .uuid(event.getPlayer().getUniqueId())
                    .name(event.getPlayer().getUsername())
                    .globalColor(-1)
                    .build()
            );
            transaction.commit();
        }
        // We should not commit transaction if not persist changes were made.
    }

}
