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
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.database.CoreTeam;

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
        CoreTeam team = plugin
                .getDatabase()
                .fetchOptionalTeamByName(teamName)
                .orElse(null);
        if (team == null) {
            source.sendMessage(CoreMessage.teamNotExist(teamName));
            return;
        }
        CorePlayer corePlayer = plugin
                .getDatabase()
                .fetchPlayerByUuid(player.getUniqueId());
        StringBuilder messageBuilder = new StringBuilder();
        while (args.hasNext()) messageBuilder.append(args.next()).append(' ');
        String message = messageBuilder.substring(0, messageBuilder.length() - 1);
        Component messageComponent = Component.empty()
                .append(Component.text("["))
                .append(CoreMessage.teamMention(team))
                .append(Component.text("]"))
                .append(Component.space())
                .append(CoreMessage.playerMention(corePlayer)
                        .append(Component.text(" > ").decorate(TextDecoration.BOLD))
                )
                .append(Component.text(message));
        team.getMembershipsIterator().forEachRemaining(member ->
                plugin.getServer()
                        .getPlayer(member.getUuid())
                        .ifPresent(memberPlayer -> memberPlayer.sendMessage(messageComponent))
        );
        plugin.getServer().getEventManager().fireAndForget(
                TeamMessageEvent
                        .builder()
                        .message(message)
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
