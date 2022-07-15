package space.cubicworld.core.util;

import lombok.Data;

@Data
public class Triple<F, S, T> {
    private final F first;
    private final S second;
    private final T third;
}
