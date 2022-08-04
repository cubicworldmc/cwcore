package space.cubicworld.core.command;

import lombok.RequiredArgsConstructor;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class CoreCommandNode<S extends CoreCommandSource> implements CoreCommand<S> {

    private final Map<String, CoreCommand<S>> commands = new ConcurrentHashMap<>();
    private final String name;

    @Override
    public void execute(S source, Iterator<String> args) {
        if (!args.hasNext()) {
            return;
        }
        String commandName = args.next().toLowerCase(Locale.ROOT);
        CoreCommand<S> command = commands.get(commandName);
        if (command == null) {
            return;
        }
        command.execute(source, args);
    }

    @Override
    public List<String> tab(S source, Iterator<String> args) {
        if (!args.hasNext()) {
            return possibleCommands(source).toList();
        }
        String commandName = args.next().toLowerCase(Locale.ROOT);
        CoreCommand<S> command = commands.get(commandName);
        if (command == null) {
            return possibleCommands(source).toList();
        }
        return command.tab(source, args);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isAdmin() {
        return false;
    }

    private Stream<String> possibleCommands(S source) {
        return commands
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().hasPermission(source.getPermission(
                        getPermission() + "." + entry.getValue().getPermission()
                )))
                .map(Map.Entry::getKey);
    }

    public CoreCommandNode command(CoreCommand<S> command) {
        commands.put(command.getName().toLowerCase(Locale.ROOT), command);
        for (String alias: command.getAliases()) {
            commands.put(alias.toLowerCase(Locale.ROOT), command);
        }
        return this;
    }

}
