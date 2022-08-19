package space.cubicworld.core.event;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.format.TextColor;
import space.cubicworld.core.color.CoreColor;
import space.cubicworld.core.database.CorePlayer;

@RequiredArgsConstructor
public class ColorChangeEvent {

    private final CorePlayer player;
    private final CoreColor previous;
    private final CoreColor current;

}
