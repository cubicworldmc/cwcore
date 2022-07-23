package space.cubicworld.core.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.permission.Tristate;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.model.CorePlayer;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

@RequiredArgsConstructor
public class CWCoreCommand implements SimpleCommand {

    private final VelocityPlugin plugin;

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        // <value_to_change> ...
        if (args.length == 0) return; // TODO print possible next argument
        try {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "rep":
                    if (!invocation.source().hasPermission("cwcore.admin.rep")) return;
                    // TODO print this scheme
                    // ... <nickname> <+|add|-|sub|=|set> <value>
                    if (args.length != 4) return;
                    String operation = args[2];
                    int value = Integer.parseInt(args[3]);
                    Optional<CorePlayer> optionalPlayer = plugin.getPlayerNameReference().get(args[1]);
                    if (optionalPlayer.isPresent()) {
                        CorePlayer player = optionalPlayer.get();
                        switch (operation.toLowerCase(Locale.ROOT)) {
                            case "+", "add" -> player.setReputation(player.getReputation() + value);
                            case "-", "sub" -> player.setReputation(player.getReputation() - value);
                            case "=", "set" -> player.setReputation(value);
                            default -> {
                                return;
                            }
                        }
                        // TODO print new value
                        plugin.getPlayerUpdater().update(player);
                    }
            }
        } catch (Exception e) {
            invocation.source().sendMessage(
                    Component.text(e.getClass().getCanonicalName())
            );
            if (e instanceof SQLException) {
                plugin.getLogger().warn("SQLException, arguments: {}:", Arrays.toString(args), e);
            }
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().getPermissionValue("cwcore.admin") == Tristate.TRUE;
    }
}
