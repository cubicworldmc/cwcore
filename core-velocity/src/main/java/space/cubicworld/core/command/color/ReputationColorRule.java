package space.cubicworld.core.command.color;

import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.format.TextColor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.util.IntegerCompare;

@Getter
@RequiredArgsConstructor
public class ReputationColorRule implements ColorRule {

    private final VelocityPlugin plugin;
    private final IntegerCompare compare;
    private final int value;
    private final TextColor color;

    @Override
    public boolean isMatch(Player player) {
        return compare.test(
                plugin.getDatabase()
                        .fetchPlayer(player.getUniqueId())
                        .orElseThrow()
                        .getReputation(),
                value
        );
    }
}
