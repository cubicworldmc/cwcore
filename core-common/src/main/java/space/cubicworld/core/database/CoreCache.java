package space.cubicworld.core.database;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
class CoreCache<K, S, M> {

    interface Functions<K, S, M> {

        S getSecondary(M model);

        K getKey(M model);

        M fetch(ResultSet resultSet) throws SQLException;

        Connection getConnection();

        String getKeyStatement();

        String getSecondaryStatement();

        void update(M model) throws SQLException;

        void remove(M model) throws SQLException;

    }

    private static final Duration DEFAULT_DURATION = Duration.ofHours(1);

    private final Map<S, Object> secondaryLocks = new ConcurrentHashMap<>();
    private final Map<K, Object> keyLocks = new ConcurrentHashMap<>();

    private final Cache<K, Optional<M>> modelCache = CacheBuilder
            .newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(DEFAULT_DURATION)
            .removalListener(this::removalListener)
            .build();
    private final Map<S, Optional<K>> secondaryCache = new ConcurrentHashMap<>();

    private final Functions<K, S, M> functions;

    private void removalListener(RemovalNotification<K, Optional<M>> notification) {
        notification.getValue().ifPresent(model ->
                secondaryCache.remove(functions.getSecondary(model))
        );
    }

    private Object defaultLock(Object key) {
        return new Object[0];
    }

    public Optional<M> fetchByKey(K key) throws SQLException {
        Optional<M> result = modelCache.getIfPresent(key);
        if (result != null) return result;
        synchronized (keyLocks.computeIfAbsent(key, this::defaultLock)) {
            result = modelCache.getIfPresent(key);
            if (result != null) {
                keyLocks.remove(key);
                return result;
            }
            try (Connection connection = functions.getConnection();
                 PreparedStatement statement = connection.prepareStatement(functions.getKeyStatement())) {
                statement.setObject(1, key instanceof UUID ? key.toString() : key);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) result = Optional.ofNullable(functions.fetch(resultSet));
                else result = Optional.empty();
                resultSet.close();
            }
            keyLocks.remove(key);
            modelCache.put(key, result);
            result.ifPresent(model -> secondaryCache.put(functions.getSecondary(model), Optional.of(key)));
        }
        return result;
    }

    public Optional<M> fetchBySecondary(S secondary) throws SQLException {
        K key = fetchKeyBySecondary(secondary);
        if (key == null) return Optional.empty();
        Optional<M> model = modelCache.getIfPresent(key);
        if (model == null) return Optional.empty();
        return model;
    }

    private K fetchKeyBySecondary(S secondary) throws SQLException {
        Optional<K> key = secondaryCache.get(secondary);
        if (key != null && key.isPresent()) return key.get();
        synchronized (secondaryLocks.computeIfAbsent(secondary, this::defaultLock)) {
            key = secondaryCache.get(secondary);
            if (key != null && key.isPresent()) {
                secondaryLocks.remove(secondary);
                return key.get();
            }
            try (Connection connection = functions.getConnection();
                 PreparedStatement statement = connection.prepareStatement(functions.getSecondaryStatement())) {
                statement.setObject(1, secondary instanceof UUID ? secondary.toString() : secondary);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    M model = functions.fetch(resultSet);
                    K modelKey = functions.getKey(model);
                    if (modelCache.getIfPresent(modelKey) != null) return modelKey;
                    synchronized (keyLocks.computeIfAbsent(modelKey, this::defaultLock)) {
                        if (modelCache.getIfPresent(modelKey) != null) return modelKey;
                        modelCache.put(modelKey, Optional.ofNullable(model));
                    }
                    keyLocks.remove(modelKey);
                    key = Optional.of(modelKey);
                }
                else key = Optional.empty();
            }
            secondaryCache.put(secondary, key);
            secondaryLocks.remove(secondary);
        }
        return key.orElse(null);
    }

    public List<M> justFetch(String sql, Object... objects) throws SQLException {
        try (Connection connection = functions.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
            int counter = 0;
            for (Object object: objects) {
                statement.setObject(++counter, object instanceof UUID ? object.toString() : object);
            }
            ResultSet resultSet = statement.executeQuery();
            List<M> result = new ArrayList<>();
            while (resultSet.next()) {
                M model = functions.fetch(resultSet);
                result.add(model);
                cacheIfNeed(model);
            }
            resultSet.close();
            return Collections.unmodifiableList(result);
        }
    }

    public void remove(M model) throws SQLException {
        functions.remove(model);
        removeCache(model);
        Object keyLock = keyLocks.get(functions.getKey(model));
        if (keyLock != null) {
            synchronized (keyLock) {
                removeCache(model);
            }
        }
        Object secondaryLock = secondaryLocks.get(functions.getSecondary(model));
        if (secondaryLock != null) {
            synchronized (secondaryLock) {
                removeCache(model);
            }
        }
    }

    private void removeCache(M model) {
        modelCache.put(functions.getKey(model), Optional.empty());
        secondaryCache.put(functions.getSecondary(model), Optional.empty());
    }

    public void update(M model) throws SQLException {
        functions.update(model);
    }

    public void cacheIfNeed(M model) {
        K key = functions.getKey(model);
        S secondary = functions.getSecondary(model);
        if (modelCache.getIfPresent(key) == null) modelCache.put(key, Optional.ofNullable(model));
        secondaryCache.putIfAbsent(secondary, Optional.of(key));
    }

    public void cache(M model) {
        modelCache.put(functions.getKey(model), Optional.of(model));
        secondaryCache.put(functions.getSecondary(model), Optional.of(functions.getKey(model)));
    }

    public void changeSecondaryKey(S previousKey, S newKey) {
        secondaryCache.put(newKey, secondaryCache.remove(previousKey));
    }

}
