package space.cubicworld.core.command.color;

import net.kyori.adventure.text.format.TextColor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.util.ColorUtils;
import space.cubicworld.core.util.IntegerCompare;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ColorRuleContainer {

    private final List<ColorRule> rules = new ArrayList<>();

    public ColorRuleContainer(VelocityPlugin plugin) {
        Map<String, String> colors = plugin.getConfig().get("colors");
        colors.forEach((key, value) -> {
            TextColor color = ColorUtils.fromLocalized(value);
            String[] elements = key.split(" ");
            String type = elements[0];
            rules.add(switch (type) {
                case "reputation", "rep" -> {
                    IntegerCompare compare = IntegerCompare.fromOperator(elements[1]);
                    int reputation = Integer.parseInt(elements[2]);
                    yield new ReputationColorRule(plugin, compare, reputation, color);
                }
                default -> throw new IllegalArgumentException("%s color type is not supported".formatted(type));
            });
        });
    }

    public ColorRule getRule(int index) {
        return rules.size() > index && index >= 0 ? rules.get(index) : null;
    }

}
