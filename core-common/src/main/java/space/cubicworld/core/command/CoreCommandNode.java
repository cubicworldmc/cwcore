package space.cubicworld.core.command;

import lombok.RequiredArgsConstructor;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class CoreCommandNode<S extends CoreCommandSource> extends AbstractCoreCommand<S> {

    private final Map<String, CoreCommand<S>> commands = new ConcurrentHashMap<>();

    @Override
    public void execute(S source, Iterator<String> args) {
        if (!args.hasNext()) {
            return;
        }
        String commandName = args.next().toLowerCase(Locale.ROOT);
        CoreCommand<S> command = commands.get(commandName);
        if (command == null || !source.hasPermission(command)) {
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
        if (command == null || !source.hasPermission(command)) {
            return possibleCommands(source).toList();
        }
        return command.tab(source, args);
    }

    private Stream<String> possibleCommands(S source) {
        return commands
                .entrySet()
                .stream()
                .filter(entry -> source.hasPermission(entry.getValue()))
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
