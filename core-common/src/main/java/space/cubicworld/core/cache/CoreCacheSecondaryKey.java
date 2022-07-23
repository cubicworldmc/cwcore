package space.cubicworld.core.cache;

import lombok.RequiredArgsConstructor;
import space.cubicworld.core.model.CoreStatement;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@RequiredArgsConstructor
public class CoreCacheSecondaryKey<K, M, V> {

    private final Map<K, M> map = new ConcurrentHashMap<>();
    private final Function<V, K> keyGetter;
    private final CoreStatement<K> loader;

    @SuppressWarnings("suspicious call")
    protected M get(Object key) {
        return map.get(key);
    }

    protected CoreStatement<K> getLoader() {
        return loader;
    }

    protected void put(M key, V value) {
        K secondaryKey = keyGetter.apply(value);
        if (secondaryKey == null) return;
        map.put(secondaryKey, key);
    }

    protected void remove(M key, V value) {
        map.remove(keyGetter.apply(value));
    }

}
