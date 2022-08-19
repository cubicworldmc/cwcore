package space.cubicworld.core.event;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.format.TextColor;
import space.cubicworld.core.database.CorePlayer;

@RequiredArgsConstructor
public class ColorChangeEvent {

    private final CorePlayer player;
    private final TextColor previous;
    private final TextColor current;

}
