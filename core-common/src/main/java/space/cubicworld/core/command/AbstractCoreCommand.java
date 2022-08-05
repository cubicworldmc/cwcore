package space.cubicworld.core.command;

import lombok.Getter;

@Getter
public abstract class AbstractCoreCommand<S> implements CoreCommand<S> {

    private final String name;
    private final String permission;
    private final String[] aliases;
    private final boolean admin;

    public AbstractCoreCommand() {
        name = CoreCommand.super.getName();
        permission = CoreCommand.super.getPermission();
        aliases = CoreCommand.super.getAliases();
        admin = CoreCommand.super.isAdmin();
    }
}
