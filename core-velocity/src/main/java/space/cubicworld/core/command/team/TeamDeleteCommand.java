package space.cubicworld.core.command.team;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.util.TriState;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.event.TeamDeleteEvent;
import space.cubicworld.core.message.CoreMessage;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@CoreCommandAnnotation(
        name = "delete",
        permission = "cwcore.team.delete"
)
@RequiredArgsConstructor
public class TeamDeleteCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

    private final VelocityPlugin plugin;

    @Override
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
        String nextArg = args.hasNext() ? args.next() : null;
        boolean force = nextArg != null && nextArg.equals("force") && source.getPermission("cwcore.team.delete.force") == TriState.TRUE;
        boolean confirm = nextArg != null && nextArg.equals("confirm");
        plugin.getDatabase()
                .fetchTeam(teamName)
                .filter(team -> team.getOwnerId().equals(player.getUniqueId()) || force)
                .ifPresentOrElse(
                        team -> {
                            if (!force && !confirm){
                                source.sendMessage(CoreMessage.teamDeleteConfirm(team));
                                return;
                            }
                            plugin.getDatabase().remove(team);
                            plugin.getServer().getEventManager().fireAndForget(
                                    new TeamDeleteEvent(team)
                            );
                            source.sendMessage(CoreMessage.teamDeleted(team));
                        },
                        () -> source.sendMessage(CoreMessage.teamDeleteCanNot(teamName))
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
}
