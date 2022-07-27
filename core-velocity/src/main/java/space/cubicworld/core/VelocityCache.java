package space.cubicworld.core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.model.CoreMember;
import space.cubicworld.core.model.CorePlayer;
import space.cubicworld.core.model.CoreTeam;

import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class VelocityCache {

    private static <V> V single(List<V> list) {
        return list.isEmpty() ? null : list.get(0);
    }

    private final VelocityPlugin plugin;
    private final Map<UUID, CorePlayer> onlinePlayers = new ConcurrentHashMap<>();
    private final Cache<UUID, CorePlayer> cachedPlayers = CacheBuilder
            .newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .maximumSize(200)
            .removalListener(this::playerCacheRemoval)
            .build();
    private final Cache<Integer, CoreTeam> cachedTeams = CacheBuilder
            .newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .removalListener(this::teamCacheRemoval)
            .maximumSize(100)
            .build();
    private final Map<String, UUID> playerNames = new ConcurrentHashMap<>();
    private final Map<Integer, List<UUID>> teamMembers = new ConcurrentHashMap<>();
    private final Map<String, Integer> teamNames = new ConcurrentHashMap<>();

    private void playerCacheRemoval(RemovalNotification<UUID, CorePlayer> notification) {
        if (onlinePlayers.containsKey(notification.getKey()) || notification.getValue() == null) return;
        playerNames.remove(notification.getValue().getName());
    }

    private void teamCacheRemoval(RemovalNotification<Integer, CoreTeam> notification) {
        teamMembers.remove(notification.getKey());
        if (notification.getValue() != null) {
            teamNames.remove(notification.getValue().getName());
        }
    }

    public void removePlayer(UUID uuid) {
        cachedPlayers.invalidate(uuid);
        CorePlayer player = onlinePlayers.remove(uuid);
        if (player != null) playerNames.remove(player.getName());
    }

    public void putIfAbsentPlayer(CorePlayer player, boolean cache) {
        if (cachedPlayers.getIfPresent(player.getUuid()) != null ||
                onlinePlayers.containsKey(player.getUuid())) return;
        putPlayer(player, cache);
    }

    public void putPlayer(CorePlayer player, boolean cache) {
        if (player == null) return;
        removePlayer(player.getUuid());
        if (cache) {
            cachedPlayers.put(player.getUuid(), player);
        } else {
            onlinePlayers.put(player.getUuid(), player);
        }
        playerNames.put(player.getName(), player.getUuid());
    }

    public CorePlayer loadPlayer(UUID uuid, boolean cache) throws SQLException {
        CorePlayer value = onlinePlayers.get(uuid);
        if (value != null) return value;
        value = cachedPlayers.getIfPresent(uuid);
        if (value != null) return value;
        value = single(CorePlayer
                .selectByUuidStatement()
                .query(plugin.getDatabase(), CorePlayer::read, uuid)
        );
        putPlayer(value, cache);
        return value;
    }

    public CorePlayer loadPlayer(String name) throws SQLException {
        UUID uuid = playerNames.get(name);
        if (uuid != null) return loadPlayer(uuid, true);
        CorePlayer player = single(CorePlayer
                .selectByNameStatement()
                .query(plugin.getDatabase(), CorePlayer::read, name)
        );
        putPlayer(player, true);
        return player;
    }

    public void removeTeam(CoreTeam team) {
        cachedTeams.invalidate(team.getId());
    }

    public void putTeam(CoreTeam team) {
        if (team == null) return;
        cachedTeams.put(team.getId(), team);
        teamNames.put(team.getName(), team.getId());
    }

    public CoreTeam loadTeam(int id) throws SQLException {
        CoreTeam value = cachedTeams.getIfPresent(id);
        if (value != null) return value;
        value = single(CoreTeam
                .selectByIdStatement()
                .query(plugin.getDatabase(), CoreTeam::read, id)
        );
        putTeam(value);
        return value;
    }

    public CoreTeam loadTeam(String name) throws SQLException {
        Integer id = teamNames.get(name);
        if (id != null) return cachedTeams.getIfPresent(id);
        CoreTeam value = single(CoreTeam
                .selectByNameStatement()
                .query(plugin.getDatabase(), CoreTeam::read, name)
        );
        putTeam(value);
        return value;
    }

    public List<UUID> loadTeamMembers(int id) throws SQLException {
        List<UUID> value = teamMembers.get(id);
        if (value != null) return value;
        List<CorePlayer> members = CoreMember
                .selectMembersByTeamIdStatement()
                .query(plugin.getDatabase(), CorePlayer::read, id);
        value = members.stream()
                .peek(player -> this.putIfAbsentPlayer(player, true))
                .map(CorePlayer::getUuid)
                .collect(Collectors.toCollection(CopyOnWriteArrayList::new));
        teamMembers.put(id, value);
        return value;
    }

}
