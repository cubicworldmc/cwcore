package space.cubicworld.core.command.team;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.event.TeamMessageEvent;
import space.cubicworld.core.message.CoreMessage;
import space.cubicworld.core.model.CorePlayer;
import space.cubicworld.core.model.CoreTeam;
import space.cubicworld.core.model.CoreTeamMember;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@CoreCommandAnnotation(
        name = "message",
        permission = "cwcore.team.message",
        aliases = {"msg", "tell", "w"}
)
@RequiredArgsConstructor
public class TeamMessageCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

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
        plugin.beginTransaction();
        CoreTeam team = plugin
                .currentSession()
                .createQuery("""
                        FROM CoreTeam team
                        INNER JOIN CoreTeamMember member ON member.link.player = :player AND member.link.team = team
                        WHERE team.name = :name
                        """, CoreTeam.class
                )
                .setParameter("player", plugin.currentSession().find(CorePlayer.class, player.getUniqueId()))
                .setParameter("name", teamName)
                .getSingleResultOrNull();
        CorePlayer corePlayer = plugin.currentSession().find(CorePlayer.class, player.getUniqueId());
        if (team == null) {
            source.sendMessage(CoreMessage.teamNotExist(teamName));
            return;
        }
        StringBuilder message = new StringBuilder();
        while (args.hasNext()) message.append(args.next()).append(' ');
        for (CoreTeamMember member : team.getMembers()) {
            Component messageComponent = Component.empty()
                    .append(Component.text("["))
                    .append(CoreMessage.teamMention(team))
                    .append(Component.text("]"))
                    .append(Component.space())
                    .append(CoreMessage.playerMention(corePlayer)
                            .append(Component.text(" > ").decorate(TextDecoration.BOLD))
                    )
                    .append(Component.text(message.toString()));
            plugin.getServer().getPlayer(member.getLink().getPlayer().getUuid())
                    .ifPresent(memberPlayer -> memberPlayer.sendMessage(messageComponent));
        }
        plugin.commitTransaction();
        plugin.getServer().getEventManager().fireAndForget(
                TeamMessageEvent
                        .builder()
                        .message(message.substring(0, message.length() - 1))
                        .build()
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
