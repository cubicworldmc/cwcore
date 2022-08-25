package space.cubicworld.core;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import space.cubicworld.core.listener.BukkitChatListener;
import space.cubicworld.core.json.CoreLightPlayer;
import space.cubicworld.core.listener.BukkitJoinQuitListener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class BukkitPlugin extends JavaPlugin {

    private final Map<UUID, CoreLightPlayer> corePlayers = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        BukkitPlayerUpdater playerUpdater = new BukkitPlayerUpdater(this);
        getServer().getPluginManager().registerEvents(playerUpdater, this);
        getServer().getPluginManager().registerEvents(new BukkitChatListener(this), this);
        getServer().getPluginManager().registerEvents(new BukkitJoinQuitListener(), this);
        getServer().getMessenger().registerIncomingPluginChannel(
                this, CoreStatic.PLAYER_UPDATE_CHANNEL, playerUpdater);
    }

}
