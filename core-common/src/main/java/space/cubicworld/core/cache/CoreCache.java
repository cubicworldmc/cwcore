package space.cubicworld.core.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import space.cubicworld.core.database.CoreDatabase;
import space.cubicworld.core.model.CoreStatement;

import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class CoreCache<K, V> {

    private final Map<K, V> permanent = new ConcurrentHashMap<>();
    private final Cache<K, V> cache;
    private final Map<Class<?>, CoreCacheSecondaryKey<?, K, V>> secondaryKeys = new ConcurrentHashMap<>();

    private final CoreStatement<K> loader;
    private final CoreStatement.SQLRead<V> read;
    private final CoreDatabase database;
    private final Function<V, K> keyGetter;
    private final Function<K, V> defaultValue;

    public CoreCache(CoreDatabase database, CoreStatement<K> loader,
                     CoreStatement.SQLRead<V> read, Function<V, K> keyGetter,
                     Function<K, V> defaultValue) {
        this(database, loader, read, keyGetter, defaultValue, 500, Duration.ofMinutes(30));
    }

    public CoreCache(CoreDatabase database, CoreStatement<K> loader,
                     CoreStatement.SQLRead<V> read, Function<V, K> keyGetter,
                     Function<K, V> defaultValue,
                     long maximumSize, Duration expireAfter) {
        cache = CacheBuilder
                .newBuilder()
                .maximumSize(maximumSize)
                .expireAfterAccess(expireAfter)
                .build();
        this.database = database;
        this.loader = loader;
        this.read = read;
        this.keyGetter = keyGetter;
        this.defaultValue = defaultValue;
    }

    public <T> void addSecondaryKey(Class<T> clazz, CoreCacheSecondaryKey<T, K, V> secondaryKey) {
        secondaryKeys.put(clazz, secondaryKey);
        forEach(secondaryKey::put);
    }

    public <T> V bySecondaryKey(Class<T> keyClass, T key) {
        CoreCacheSecondaryKey<?, K, V> secondaryKey = secondaryKeys.get(keyClass);
        if (secondaryKey == null) {
            throw new IllegalArgumentException("This class is not register as secondary key");
        }
        K localKey = secondaryKey.get(key);
        if (localKey == null) return null;
        return get(localKey).orElseGet(() -> defaultValue.apply(localKey));
    }

    private V load(K key, Consumer<V> put) throws SQLException {
        V value = permanent.get(key);
        if (value == null) value = cache.getIfPresent(key);
        if (value == null) {
            List<V> result = loader.query(database, read, key);
            V loadedValue = result.isEmpty() ? defaultValue.apply(key) : result.get(0);
            if (loadedValue != null) {
                secondaryKeys.forEach((clazz, secondaryKey) -> secondaryKey.put(key, loadedValue));
            }
            value = loadedValue;
            put.accept(value);
        }
        return value;
    }

    private void put(K key, V value) {
        if (value != null) {
            secondaryKeys.forEach((clazz, secondaryKey) -> secondaryKey.put(key, value));
        }
    }

    public <T> void readPermanent(T statementKey, CoreStatement<T> statement) throws SQLException {
        List<V> values = statement.query(database, read, statementKey);
        if (!values.isEmpty()) {
            V value = values.get(0);
            putPermanent(keyGetter.apply(value), value);
        }
    }

    public <T> void readCache(T statementKey, CoreStatement<T> statement) throws SQLException {
        List<V> values = statement.query(database, read, statementKey);
        if (!values.isEmpty()) {
            V value = values.get(0);
            putCache(keyGetter.apply(value), value);
        }
    }

    public void putPermanent(K key, V value) {
        put(key, value);
        permanent.put(key, value);
    }

    public void putCache(K key, V value) {
        put(key, value);
        cache.put(key, value);
    }

    public V loadPermanent(K key) throws SQLException {
        V cacheValue = cache.getIfPresent(key);
        if (cacheValue != null) {
            permanent.put(key, cacheValue);
            return cacheValue;
        }
        return load(key, value -> permanent.put(key, value));
    }

    public V loadCache(K key) throws SQLException {
        return load(key, value -> cache.put(key, value));
    }

    private void remove(K key, V value) {
        if (value == null) return;
        secondaryKeys.forEach((clazz, secondaryKey) -> secondaryKey.remove(key, value));
    }

    public V removePermanent(K key) {
        V value = permanent.remove(key);
        value = value == null ? defaultValue.apply(key) : value;
        remove(key, value);
        return value;
    }

    public V removeCache(K key) {
        V value = cache.getIfPresent(key);
        if (value == null) value = defaultValue.apply(key);
        else cache.invalidate(key);
        remove(key, value);
        return value;
    }

    public V remove(K key) {
        V permanentValue = permanent.remove(key);
        V cacheValue = cache.getIfPresent(key);
        if (cacheValue != null) {
            remove(key, cacheValue);
            cache.invalidate(key);
            return cacheValue;
        }
        permanentValue = permanentValue == null ? defaultValue.apply(key) : permanentValue;
        remove(key, permanentValue);
        return permanentValue;
    }

    public Optional<V> get(K key) {
        return Optional.ofNullable(permanent.get(key))
                .or(() -> Optional.ofNullable(cache.getIfPresent(key)));
    }

    public void forEach(BiConsumer<K, V> consumer) {
        permanent.forEach(consumer);
        cache.asMap().forEach(consumer);
    }

}
