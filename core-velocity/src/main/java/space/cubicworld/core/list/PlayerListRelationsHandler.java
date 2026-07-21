package space.cubicworld.core.list;

import com.electronwill.nightconfig.core.Config;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import net.kyori.adventure.text.Component;
import reactor.core.Disposable;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.message.CoreMessage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerListRelationsHandler {

    private final VelocityPlugin plugin;

    private final Map<UUID, Disposable> requests = new ConcurrentHashMap<>();
    private final Map<String, List<Integer>> serverRequirements;

    public PlayerListRelationsHandler(VelocityPlugin plugin) {
        this.plugin = plugin;
        serverRequirements = new HashMap<>();
        Map<String, Object> lists = plugin.getConfig().<Config>get("lists").valueMap();
        for (String list : lists.keySet()) {
            int listId = plugin.getCore().getListContainer().getList(list).getId();
            for (String server : plugin
                    .getConfig()
                    .<List<String>>get(String.format("lists.%s.required-to-join", list))
            ) {
                serverRequirements.putIfAbsent(server, new ArrayList<>());
                serverRequirements.get(server).add(listId);
            }
        }
    }

    @Subscribe
    public void join(PlayerChooseInitialServerEvent event) {
        Disposable disposable = plugin.getCore().getListContainer().cachePlayer(event.getPlayer().getUniqueId()).subscribe();
        requests.put(event.getPlayer().getUniqueId(), disposable);
    }

    @Subscribe
    public void quit(DisconnectEvent event) {
        Disposable disposable = requests.remove(event.getPlayer().getUniqueId());
        if (disposable != null) disposable.dispose();
        plugin.getCore().getListContainer().uncachePlayer(event.getPlayer().getUniqueId());
    }

    @Subscribe(priority = Short.MIN_VALUE)
    public void serverConnect(ServerPreConnectEvent event) {
        event.getResult().getServer().ifPresent(server -> {
            String serverName = server.getServerInfo().getName();
            List<Integer> lists = serverRequirements.get(serverName);
            if (lists == null || lists.isEmpty()) return;
            UUID uuid = event.getPlayer().getUniqueId();
            Set<Integer> acceptedRelations = plugin.getCore().getListContainer().getPlayerCache(uuid);
            if (acceptedRelations == null) {
                VelocityCoreCommandSource.sendLocaleMessage(
                        event.getPlayer(),
                        CoreMessage.listNotYetLoaded()
                );
                event.setResult(ServerPreConnectEvent.ServerResult.denied());
                return;
            }
            for (Integer listId : lists) {
                if (!acceptedRelations.contains(listId)) {
                    CoreListData listData = plugin.getCore().getListContainer().getList(listId);
                    VelocityCoreCommandSource.sendLocaleMessage(
                            event.getPlayer(),
                            Component.empty().append(
                                    CoreMessage.listAcceptedRequiredToJoinServer(
                                            listData.getName(),
                                            serverName
                                    ),
                                    Component.newline(),
                                    listData
                                            .getAcceptance()
                                            .acceptanceInstructionsMessage(
                                                    listData.getName(), event.getPlayer().getGameProfile().getName()
                                            )
                            )
                    );
                    event.setResult(ServerPreConnectEvent.ServerResult.denied());
                    return;
                }
            }
        });
    }

}