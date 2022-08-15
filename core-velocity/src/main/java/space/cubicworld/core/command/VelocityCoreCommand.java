package space.cubicworld.core.command;

import com.velocitypowered.api.command.SimpleCommand;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.VelocityPlugin;

import java.util.List;

@RequiredArgsConstructor
public class VelocityCoreCommand implements SimpleCommand {

    private final CoreCommand<VelocityCoreCommandSource> command;

    public void register(VelocityPlugin plugin) {
        plugin.getServer().getCommandManager().register(
                command.getName(),
                this,
                command.getAliases()
        );
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return command.tab(
                new VelocityCoreCommandSource(invocation.source()),
                List.of(invocation.arguments()).iterator()
        );
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        VelocityCoreCommandSource source = new VelocityCoreCommandSource(invocation.source());
        return source.hasPermission(command);
    }

    @Override
    public void execute(Invocation invocation) {
        command.execute(
                new VelocityCoreCommandSource(invocation.source()),
                List.of(invocation.arguments()).iterator()
        );
    }
}
