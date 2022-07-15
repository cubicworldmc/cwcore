package space.cubicworld.core.util;

import lombok.Data;

@Data
public class Pair<T, V> {

    private final T first;
    private final V second;

}
