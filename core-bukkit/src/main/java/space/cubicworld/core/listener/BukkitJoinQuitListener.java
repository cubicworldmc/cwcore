package space.cubicworld.core.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class BukkitJoinQuitListener implements Listener {

    @EventHandler
    public void join(PlayerJoinEvent event) {
        event.joinMessage(null);
    }

    @EventHandler
    public void quit(PlayerQuitEvent event) {
        event.quitMessage(null);
    }

}
