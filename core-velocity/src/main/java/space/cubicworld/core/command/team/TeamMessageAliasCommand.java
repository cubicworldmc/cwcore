package space.cubicworld.core.command.team;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
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
import java.util.Objects;

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
        plugin.getDatabase()
                .fetchPlayer(player.getUniqueId())
                .flatMap(corePlayer -> corePlayer
                        .getSelectedTeam()
                        .flatMap(selectedTeam -> selectedTeam == null ?
                                Mono.just(CoreMessage.selectTeamNeed()) :
                                plugin.getDatabase()
                                        .fetchPTRelation(corePlayer.getId(), selectedTeam.getId())
                                        .flatMap(relation -> {
                                            if (relation.getValue() != CorePTRelation.Value.MEMBERSHIP) {
                                                return CoreMessage.teamNotMemberSelf(selectedTeam);
                                            }
                                            String message = MessageUtils.buildMessage(args);
                                            if (message == null) {
                                                return Mono.just(CoreMessage.teamMessageEmpty());
                                            }
                                            plugin.getServer().getEventManager().fireAndForget(
                                                    new TeamMessageEvent(
                                                            corePlayer,
                                                            selectedTeam,
                                                            message
                                                    )
                                            );
                                            return Mono.empty();
                                        })
                        )
                )
                .doOnNext(source::sendMessage)
                .doOnError(this.errorLog(plugin.getLogger()))
                .subscribe();
    }

    @Override
    public List<String> tab(VelocityCoreCommandSource source, Iterator<String> args) {
        return Collections.emptyList();
    }
}
