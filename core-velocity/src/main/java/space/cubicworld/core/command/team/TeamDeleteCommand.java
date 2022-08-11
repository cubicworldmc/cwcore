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
import space.cubicworld.core.model.CoreTeam;

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
        CoreTeam team = plugin
                .getTeamByName()
                .getOptionalModel(teamName)
                .orElse(null);
        if (team == null || !team.getOwner().getUuid().equals(player.getUniqueId())) {
            source.sendMessage(CoreMessage.teamDeleteCanNot(teamName));
            return;
        }
        String nextArg = args.hasNext() ? args.next() : null;
        if (nextArg == null || (!nextArg.equals("confirm") &&
                !(nextArg.equals("force") && source.getPermission("cwcore.team.delete.force") == TriState.TRUE))
        ) {
            source.sendMessage(CoreMessage.teamDeleteConfirm(team));
            return;
        }
        plugin.beginTransaction();
        plugin.currentSession().remove(team);
        plugin.commitTransaction();
        plugin.getServer().getEventManager().fireAndForget(
                TeamDeleteEvent
                        .builder()
                        .team(team)
                        .build()
        );
        source.sendMessage(CoreMessage.teamDeleted(team));
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
