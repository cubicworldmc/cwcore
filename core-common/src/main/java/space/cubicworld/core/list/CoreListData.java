package space.cubicworld.core.list;

import lombok.Getter;
import space.cubicworld.core.CorePlugin;

import java.util.Objects;

@Getter
public class CoreListData {

    private final String name;
    private final int id;
    private final CoreListAcceptance acceptance;

    public CoreListData(CorePlugin plugin, String name, int id) {
        this.name = name;
        this.id = id;
        String acceptanceKind = plugin.getConfig().get("lists.%s.acceptance.kind".formatted(name));
        switch (Objects.requireNonNullElse(acceptanceKind, "none")) {
            case "none" -> acceptance = new CoreListAcceptance.None();
            case "lc" -> acceptance = new CoreListAcceptance.LinkCode(plugin, name);
            default -> throw new IllegalArgumentException("%s is unknown acceptance kind".formatted(acceptanceKind));
        }
    }

}
