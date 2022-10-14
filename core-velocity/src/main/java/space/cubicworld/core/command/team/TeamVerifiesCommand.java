package space.cubicworld.core.command.team;

import com.velocitypowered.api.event.Subscribe;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.event.TeamCreateEvent;
import space.cubicworld.core.event.TeamDeleteEvent;
import space.cubicworld.core.event.TeamVerifyEvent;
import space.cubicworld.core.message.CoreMessage;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@CoreCommandAnnotation(
        name = "verifies",
        permission = "cwcore.team.verifies",
        admin = true
)
@RequiredArgsConstructor
public class TeamVerifiesCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

    private final VelocityPlugin plugin;

    @Override
    public void execute(VelocityCoreCommandSource source, Iterator<String> args) {
        int page = args.hasNext() ? Integer.parseInt(args.next()) - 1 : 0;
        CoreMessage.verifies(plugin.getDatabase(), page)
                .doOnNext(source::sendMessage)
                .doOnError(this.errorLog(plugin.getLogger()))
                .subscribe();
    }

    @Override
    public List<String> tab(VelocityCoreCommandSource source, Iterator<String> args) {
        return Collections.emptyList();
    }

}
