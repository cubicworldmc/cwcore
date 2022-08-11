package space.cubicworld.core.command.team;

import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.event.TeamVerifyEvent;
import space.cubicworld.core.message.CoreMessage;
import space.cubicworld.core.model.CoreTeam;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@CoreCommandAnnotation(
        name = "verify",
        permission = "cwcore.team.verify",
        admin = true
)
@RequiredArgsConstructor
public class TeamVerifyCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

    private final VelocityPlugin plugin;

    @Override
    public void execute(VelocityCoreCommandSource source, Iterator<String> args) {
        if (!args.hasNext()) {
            source.sendMessage(CoreMessage.provideTeamName());
            return;
        }
        String teamName = args.next();
        CoreTeam team = plugin.getTeamByName()
                .getOptionalModel(teamName)
                .orElse(null);
        if (team == null) {
            source.sendMessage(CoreMessage.teamNotExist(teamName));
            return;
        }
        if (team.isVerified()) {
            source.sendMessage(CoreMessage.teamAlreadyVerified(team));
            return;
        }
        team.setVerified(true);
        plugin.beginTransaction();
        plugin.currentSession().persist(team);
        plugin.commitTransaction();
        plugin.getServer().getEventManager().fireAndForget(
                TeamVerifyEvent
                        .builder()
                        .verifier(source.getSource())
                        .team(team)
                        .build()
        );
        source.sendMessage(CoreMessage.teamVerifiedSet(team));
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
