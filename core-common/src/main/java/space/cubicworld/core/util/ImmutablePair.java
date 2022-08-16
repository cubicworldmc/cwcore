package space.cubicworld.core.util;

import lombok.Data;

@Data
public class ImmutablePair<T, V> {

    private final T first;
    private final V second;

}
