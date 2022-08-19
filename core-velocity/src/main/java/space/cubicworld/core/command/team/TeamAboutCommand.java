package space.cubicworld.core.command.team;

import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.database.CorePTRelation;
import space.cubicworld.core.message.CoreMessage;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@CoreCommandAnnotation(
        name = "about",
        permission = "cwcore.team.about"
)
@RequiredArgsConstructor
public class TeamAboutCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

    private final VelocityPlugin plugin;

    @Override
    public void execute(VelocityCoreCommandSource source, Iterator<String> args) {
        if (!args.hasNext()) {
            source.sendMessage(CoreMessage.provideTeamName());
            return;
        }
        String teamName = args.next();
        plugin.getDatabase()
                .fetchTeam(teamName)
                .ifPresentOrElse(
                        team -> source.sendMessage(CoreMessage.teamAbout(
                                team,
                                (source.getSource().getPermissionValue("cwcore.team.about.hide.ignore") == Tristate.TRUE) ||
                                (source.getSource() instanceof Player player && plugin.getDatabase()
                                        .fetchPTRelation(player.getUniqueId(), team.getId())
                                        .orElseThrow()
                                        .getValue() == CorePTRelation.Value.MEMBERSHIP)
                        )),
                        () -> source.sendMessage(CoreMessage.teamNotExist(teamName))
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
