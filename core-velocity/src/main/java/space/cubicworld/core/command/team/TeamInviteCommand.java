package space.cubicworld.core.command.team;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.database.CorePTRelation;
import space.cubicworld.core.event.TeamInviteEvent;
import space.cubicworld.core.message.CoreMessage;

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
        plugin
                .getDatabase()
                .fetchTeam(teamName)
                .filter(team -> team.getOwnerId().equals(player.getUniqueId()))
                .flatMap(team -> plugin.getDatabase()
                        .fetchPlayer(playerName)
                        .flatMap(invited -> plugin.getDatabase()
                                .fetchPTRelation(invited.getId(), team.getId())
                                .flatMap(relation ->
                                        switch (relation.getValue()) {
                                            case INVITE, READ -> CoreMessage.teamInvitedAlready(invited, team);
                                            case MEMBERSHIP -> CoreMessage.alreadyInTeam(invited, team);
                                            case NONE -> {
                                                relation.setValue(CorePTRelation.Value.INVITE);
                                                yield plugin.getDatabase().update(relation)
                                                        .doOnSuccess(it -> plugin.getServer().getEventManager().fireAndForget(
                                                                new TeamInviteEvent(player, invited, team)
                                                        ))
                                                        .then(CoreMessage.teamInvitationSend(invited, team));
                                            }
                                        }
                                )
                        )
                        .defaultIfEmpty(CoreMessage.playerNotExist(playerName))
                )
                .defaultIfEmpty(CoreMessage.teamNotExist(teamName))
                .doOnNext(source::sendMessage)
                .doOnError(this.errorLog(plugin.getLogger()))
                .subscribe();
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
