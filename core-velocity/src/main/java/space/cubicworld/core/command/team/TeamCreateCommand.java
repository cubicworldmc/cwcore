package space.cubicworld.core.command.team;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.Player;
import lombok.SneakyThrows;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.database.CorePTRelation;
import space.cubicworld.core.database.CoreTeam;
import space.cubicworld.core.event.TeamCreateEvent;
import space.cubicworld.core.event.TeamDeleteEvent;
import space.cubicworld.core.event.TeamVerifyEvent;
import space.cubicworld.core.message.CoreMessage;

import java.time.Duration;
import java.util.*;

@CoreCommandAnnotation(
        name = "create",
        permission = "cwcore.team.create"
)
public class TeamCreateCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

    private final VelocityPlugin plugin;

    private final LoadingCache<UUID, Optional<Integer>> verifiedCache = CacheBuilder
            .newBuilder()
            .maximumSize(100)
            .expireAfterAccess(Duration.ofMinutes(10))
            .build(verifiedCacheLoader());

    public TeamCreateCommand(VelocityPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getEventManager().register(plugin, this);
    }

    private CacheLoader<UUID, Optional<Integer>> verifiedCacheLoader() {
        return CacheLoader.from(uuid -> {
            List<CoreTeam> teams = plugin.getDatabase()
                    .fetchTeams(
                            "SELECT * FROM teams WHERE verified = false AND owner_uuid = ?",
                            uuid
                    );
            if (teams.isEmpty()) return Optional.empty();
            else return Optional.of(teams.get(0).getId());
        });
    }

    @Override
    @SneakyThrows
    public void execute(VelocityCoreCommandSource source, Iterator<String> args) {
        if (!args.hasNext()) {
            source.sendMessage(CoreMessage.provideTeamName());
            return;
        }
        String teamName = args.next();
        if (!(source.getSource() instanceof Player player)) {
            source.sendMessage(CoreMessage.forPlayer());
            return;
        }
        verifiedCache
                .get(player.getUniqueId())
                .flatMap(id -> plugin.getDatabase().fetchTeam(id))
                .ifPresentOrElse(
                        team -> source.sendMessage(CoreMessage.oneTeamNotVerified(team)),
                        () -> {
                            if (plugin.getDatabase().fetchTeam(teamName).isEmpty()) {
                                CoreTeam team = plugin.getDatabase().newTeam(teamName, player.getUniqueId());
                                CorePTRelation relation = plugin.getDatabase()
                                        .fetchPTRelation(player.getUniqueId(), team.getId())
                                        .orElseThrow();
                                relation.setValue(CorePTRelation.Value.MEMBERSHIP);
                                plugin.getDatabase().update(relation);
                                plugin.getServer().getEventManager().fireAndForget(
                                        new TeamCreateEvent(player, team)
                                );
                                source.sendMessage(CoreMessage.teamCreated(team));
                            } else {
                                source.sendMessage(CoreMessage.teamAlreadyExist(teamName));
                            }
                        }
                );
    }

    @Override
    public List<String> tab(VelocityCoreCommandSource source, Iterator<String> args) {
        if (!args.hasNext()) {
            return Collections.emptyList();
        }
        args.next();
        if (!args.hasNext()) {
            return Collections.singletonList("<team_name>");
        }
        return Collections.emptyList();
    }

    @Subscribe
    public void teamVerify(TeamVerifyEvent event) {
        verifiedCache.invalidate(event.getTeam().getOwnerId());
    }

    @Subscribe
    public void teamCreate(TeamCreateEvent event) {
        if (!event.getTeam().isVerified()) {
            verifiedCache.put(event.getTeam().getOwnerId(), Optional.of(event.getTeam().getId()));
        }
    }

    @Subscribe
    public void teamDelete(TeamDeleteEvent event) {
        if (!event.getTeam().isVerified()) {
            verifiedCache.invalidate(event.getTeam().getOwnerId());
        }
    }

}
