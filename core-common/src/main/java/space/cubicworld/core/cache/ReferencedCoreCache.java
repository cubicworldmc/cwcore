package space.cubicworld.core.cache;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.model.CoreStatement;

import java.sql.SQLException;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public class ReferencedCoreCache<K, V> {

    private final CoreCache<?, V> realCoreCache;
    private final Class<K> secondaryKeyClass;
    private final CoreStatement<K> statement;

    public Optional<V> get(K key) throws SQLException {
        V value = realCoreCache.bySecondaryKey(secondaryKeyClass, key);
        if (value == null) {
            if (statement != null) {
                realCoreCache.readCache(key, statement);
                return Optional.ofNullable(
                        realCoreCache.bySecondaryKey(secondaryKeyClass, key)
                );
            }
            return Optional.empty();
        }
        return Optional.of(value);
    }

}
