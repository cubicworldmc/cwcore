package space.cubicworld.core.listener;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import space.cubicworld.core.BukkitPlugin;
import space.cubicworld.core.model.CorePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@RequiredArgsConstructor
public class BukkitPlayerLoader implements Listener {

    @RequiredArgsConstructor
    private static class Loader implements Runnable {

        private final Map<String, Boolean> tasks = new ConcurrentHashMap<>();
        private final Queue<String> queue = new ConcurrentLinkedDeque<>();
        private final BukkitPlugin plugin;

        public void addLoad(String name) {
            tasks.put(name, true);
            queue.add(name);
        }

        public void addUnload(String name) {
            tasks.put(name, false);
            queue.add(name);
        }

        @Override
        @SneakyThrows
        public void run() {
            int counter = 0;
            while (counter != 8 && queue.peek() != null) {
                String name = queue.poll();
                Boolean task = tasks.remove(name);
                if (task == null) continue;
                if (task) {
                    plugin.getPlayers().put(
                            name,
                            CorePlayer
                                    .selectByNameStatement()
                                    .query(plugin.getDatabase(), CorePlayer::read, name)
                                    .get(0)
                    );
                }
                else {
                    plugin.getPlayers().remove(name);
                }
                counter++;
            }
            Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, this);
        }
    }

    private final Loader loader;

    public BukkitPlayerLoader(BukkitPlugin plugin) {
        this.loader = new Loader(plugin);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, loader);
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        loader.addLoad(event.getPlayer().getName());
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
        loader.addUnload(event.getPlayer().getName());
    }

}
