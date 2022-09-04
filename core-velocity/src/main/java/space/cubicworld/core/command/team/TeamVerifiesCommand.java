package space.cubicworld.core.command.team;

import com.velocitypowered.api.event.Subscribe;
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
public class TeamVerifiesCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {

    private final VelocityPlugin plugin;

    private final List<Integer> unVerified = Collections.synchronizedList(new ArrayList<>());

    public TeamVerifiesCommand(VelocityPlugin plugin) {
        this.plugin = plugin;
        try (Connection connection = plugin.getDatabase().getConnection();
             Statement statement = connection.createStatement()
        ) {
            ResultSet resultSet = statement.executeQuery("SELECT id FROM teams WHERE verified = false");
            while (resultSet.next()) {
                unVerified.add(resultSet.getInt(1));
            }
            resultSet.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        plugin.getServer().getEventManager().register(plugin, this);
    }

    @Override
    public void execute(VelocityCoreCommandSource source, Iterator<String> args) {
        int page = args.hasNext() ? Integer.parseInt(args.next()) - 1: 0;
        source.sendMessage(CoreMessage.verifies(plugin.getDatabase(), page, unVerified));
    }

    @Override
    public List<String> tab(VelocityCoreCommandSource source, Iterator<String> args) {
        return Collections.emptyList();
    }

    @Subscribe
    public void verify(TeamVerifyEvent event) {
        unVerified.remove((Object) event.getTeam().getId());
    }

    @Subscribe
    public void create(TeamCreateEvent event) {
        if (!event.getTeam().isVerified()) {
            unVerified.add(event.getTeam().getId());
        }
    }

    @Subscribe
    public void delete(TeamDeleteEvent event) {
        if (!event.getTeam().isVerified()) {
            unVerified.remove(event.getTeam().getId());
        }
    }
}
