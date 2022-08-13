package space.cubicworld.core.command.team;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.database.CoreTeam;
import space.cubicworld.core.event.TeamCreateEvent;
import space.cubicworld.core.message.CoreMessage;

import java.time.Duration;
import java.util.*;

@CoreCommandAnnotation(
        name = "create",
        permission = "cwcore.team.create"
)
@RequiredArgsConstructor
public class TeamCreateCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

    private final VelocityPlugin plugin;

    private final LoadingCache<UUID, Optional<Integer>> verifiedCache = CacheBuilder
            .newBuilder()
            .maximumSize(100)
            .expireAfterAccess(Duration.ofHours(1))
            .build(verifiedCacheLoader());

    private CacheLoader<UUID, Optional<Integer>> verifiedCacheLoader() {
        return CacheLoader.from(uuid -> {
            CoreTeam team = plugin.getDatabase()
                    .fetchTeamQuery(
                            "SELECT * FROM teams WHERE verified = false AND owner_uuid = ?",
                            uuid.toString()
                    );
            if (team == null) return Optional.empty();
            if (plugin.getDatabase().getTeamsCache().getIfPresent(team.getId()) == null) {
                plugin.getDatabase().cache(team);
            }
            return Optional.of(team.getId());
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
        Optional<CoreTeam> notVerifiedTeam = verifiedCache
                .get(player.getUniqueId())
                .map(id -> plugin.getDatabase().fetchTeamById(id));
        if (notVerifiedTeam.isPresent()) {
            source.sendMessage(CoreMessage.oneTeamNotVerified(notVerifiedTeam.get()));
            return;
        }
        if (plugin.getDatabase().fetchOptionalTeamByName(teamName).isEmpty()) {
            CoreTeam team = plugin.getDatabase().newTeam(teamName);
            team.setOwnerRaw(player.getUniqueId());
            team.addMembershipRaw(player.getUniqueId());
            team.update();
            plugin.getServer().getEventManager().fireAndForget(
                    TeamCreateEvent
                            .builder()
                            .owner(player)
                            .team(team)
                            .build()
            );
            source.sendMessage(CoreMessage.teamCreated(team));
        } else {
            source.sendMessage(CoreMessage.teamAlreadyExist(teamName));
        }
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
}
