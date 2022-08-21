package space.cubicworld.core.command;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class CoreCommandNode<S extends CoreCommandSource> extends AbstractCoreCommand<S> {

    @AllArgsConstructor
    private static final class JoiningIterator<T> implements Iterator<T> {

        private T first;
        private final Iterator<T> cont;

        @Override
        public T next() {
            return first == null ? cont.next() : first;
        }

        @Override
        public boolean hasNext() {
            return first != null && cont.hasNext();
        }
    }

    private final Map<String, CoreCommand<S>> commands = new ConcurrentHashMap<>();

    private final CoreCommand<S> defaultCommand;
    private final boolean defaultOverride;

    public CoreCommandNode() {
        this.defaultCommand = null;
        this.defaultOverride = false;
    }

    @Override
    public void execute(S source, Iterator<String> args) {
        if (!args.hasNext()) {
            executeDefaultCommand(source, args);
            return;
        }
        String commandName = args.next().toLowerCase(Locale.ROOT);
        CoreCommand<S> command = commands.get(commandName);
        if (command == null || !source.hasPermission(command)) {
            executeDefaultCommand(source, new JoiningIterator<>(commandName, args));
            return;
        }
        command.execute(source, args);
    }

    @Override
    public List<String> tab(S source, Iterator<String> args) {
        if (!args.hasNext()) {
            return defaultTab(source, args);
        }
        String commandName = args.next().toLowerCase(Locale.ROOT);
        CoreCommand<S> command = commands.get(commandName);
        if (command == null || !source.hasPermission(command)) {
            return defaultTab(source, new JoiningIterator<>(commandName, args));
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

    private void executeDefaultCommand(S source, Iterator<String> args) {
        if (defaultCommand != null) {
            defaultCommand.execute(source, args);
        }
    }

    private List<String> defaultTab(S source, Iterator<String> args) {
        return defaultOverride ?
                defaultCommand.tab(source, args) : possibleCommands(source).toList();
    }

}
