package space.cubicworld.core.command.team;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.event.TeamInviteEvent;
import space.cubicworld.core.message.CoreMessage;
import space.cubicworld.core.model.CorePlayer;
import space.cubicworld.core.model.CorePlayerTeamLink;
import space.cubicworld.core.model.CoreTeam;
import space.cubicworld.core.model.CoreTeamInvitation;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@CoreCommandAnnotation(
        name = "invite",
        permission = "cwcore.team.invite",
        aliases = "i"
)
@RequiredArgsConstructor
public class TeamInviteCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

    private final VelocityPlugin plugin;

    @Override
    public void execute(VelocityCoreCommandSource source, Iterator<String> args) {
        if (!args.hasNext()) {
            source.sendMessage(CoreMessage.provideTeamName());
            return;
        }
        String teamName = args.next();
        if (!args.hasNext()) {
            source.sendMessage(CoreMessage.providePlayerName());
            return;
        }
        String playerName = args.next();
        if (!(source.getSource() instanceof Player player)) {
            source.sendMessage(CoreMessage.forPlayer());
            return;
        }
        CoreTeam team = plugin
                .getTeamByName()
                .getOptionalModel(teamName)
                .orElse(null);
        if (team == null || !team.getOwner().getUuid().equals(player.getUniqueId())) {
            source.sendMessage(CoreMessage.teamNotExist(teamName));
            return;
        }
        CorePlayer invited = plugin
                .getPlayerByName()
                .getOptionalModel(playerName)
                .orElse(null);
        if (invited == null) {
            source.sendMessage(CoreMessage.playerNotExist(playerName));
            return;
        }
        CorePlayerTeamLink link = new CorePlayerTeamLink(invited, team);
        plugin.beginTransaction();
        if (plugin.currentSession().get(CoreTeamInvitation.class, link) != null) {
            source.sendMessage(CoreMessage.teamInvitedAlready(invited, team));
            return;
        }
        CoreTeamInvitation invitation = new CoreTeamInvitation(link);
        team.getInvitations().add(invitation);
        plugin.currentSession().persist(invitation);
        plugin.commitTransaction();
        plugin.getServer().getEventManager().fireAndForget(
                TeamInviteEvent
                        .builder()
                        .invited(invited)
                        .inviter(player)
                        .team(team)
                        .build()
        );
        source.sendMessage(CoreMessage.teamInvitationSend(invited, team));
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
        args.next();
        if (!args.hasNext()) {
            return plugin.commandHelper().playersTab();
        }
        return Collections.emptyList();
    }
}
