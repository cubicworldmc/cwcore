package space.cubicworld.core.color;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.message.CoreMessage;
import space.cubicworld.core.util.IntegerCompare;

@RequiredArgsConstructor
public class ReputationColorRule implements ColorRule {

    private final IntegerCompare compare;
    private final int value;

    @Override
    public boolean isMatches(CorePlayer player) {
        return compare.test(player.getReputation(), value);
    }

    @Override
    public Component getMessage() {
        return Component.translatable("cwcore.color.rule.reputation")
                .args(
                        Component.text(compare.toString()),
                        Component.text(value)
                )
                .color(CoreMessage.INFORMATION_COLOR);
    }
}
