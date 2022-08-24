package space.cubicworld.core.command.team;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.database.CorePTRelation;
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.database.CoreTeam;
import space.cubicworld.core.event.TeamMessageEvent;
import space.cubicworld.core.message.CoreMessage;
import space.cubicworld.core.util.MessageUtils;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@CoreCommandAnnotation(
        name = "tmsg",
        permission = "cwcore.team.message"
)
@RequiredArgsConstructor
public class TeamMessageAliasCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

    private final VelocityPlugin plugin;

    @Override
    public void execute(VelocityCoreCommandSource source, Iterator<String> args) {
        if (!(source.getSource() instanceof Player player)) {
            source.sendMessage(CoreMessage.forPlayer());
            return;
        }
        CorePlayer corePlayer = plugin.getDatabase()
                .fetchPlayer(player.getUniqueId())
                .orElseThrow();
        CoreTeam selectedTeam = corePlayer.getSelectedTeam();
        if (selectedTeam == null) {
            source.sendMessage(CoreMessage.selectTeamNeed());
            return;
        }
        CorePTRelation relation = plugin.getDatabase()
                .fetchPTRelation(corePlayer.getId(), selectedTeam.getId())
                .orElseThrow();
        if (relation.getValue() != CorePTRelation.Value.MEMBERSHIP) {
            source.sendMessage(CoreMessage.teamNotMemberSelf(selectedTeam));
            return;
        }
        String message = MessageUtils.buildMessage(args);
        if (message == null) {
            source.sendMessage(CoreMessage.teamMessageEmpty());
            return;
        }
        plugin.getServer().getEventManager().fireAndForget(
                new TeamMessageEvent(
                        corePlayer,
                        selectedTeam,
                        message
                )
        );
    }

    @Override
    public List<String> tab(VelocityCoreCommandSource source, Iterator<String> args) {
        return Collections.emptyList();
    }
}
