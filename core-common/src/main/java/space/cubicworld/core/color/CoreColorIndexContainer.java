package space.cubicworld.core.color;

import com.electronwill.nightconfig.core.AbstractConfig;
import net.kyori.adventure.text.format.TextColor;
import space.cubicworld.core.CorePlugin;
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.util.ColorUtils;
import space.cubicworld.core.util.ImmutablePair;
import space.cubicworld.core.util.IntegerCompare;

import java.util.*;

public class CoreColorIndexContainer {

    private final List<ImmutablePair<ColorRule, TextColor>> colors = new ArrayList<>();

    public CoreColorIndexContainer(CorePlugin plugin) {
        AbstractConfig colorsConfig = plugin.getConfig().get("colors");
        Map<String, String> colorsMap = new LinkedHashMap<>();
        colorsConfig.valueMap().forEach((key, value) -> colorsMap.put(key, value.toString()));
        colorsMap.forEach((key, value) -> {
            String[] keyStatement = key.split(" ");
            ColorRule rule = switch (keyStatement[0].toLowerCase(Locale.ROOT)) {
                case "reputation", "rep" -> {
                    ImmutablePair<IntegerCompare, Integer> compare = comparing(keyStatement, 1);
                    yield new ReputationColorRule(compare.getFirst(), compare.getSecond());
                }
                default -> throw new IllegalArgumentException("unknown type of value: " + keyStatement[0]);
            };
            TextColor color = ColorUtils.fromLocalized(value);
            colors.add(new ImmutablePair<>(rule, color));
        });
    }

    private ImmutablePair<IntegerCompare, Integer> comparing(String[] statements, int start) {
        return new ImmutablePair<>(
                IntegerCompare.fromOperator(statements[start++]),
                Integer.parseInt(statements[start])
        );
    }

    public Optional<TextColor> getColor(int index, CorePlayer player) {
        if (colors.size() <= index || index < 0) return Optional.empty();
        ImmutablePair<ColorRule, TextColor> color = colors.get(index);
        if (color.getFirst().isMatches(player)) return Optional.of(color.getSecond());
        return Optional.empty();
    }

    public List<ImmutablePair<ColorRule, TextColor>> getColors() {
        return Collections.unmodifiableList(colors);
    }
}
