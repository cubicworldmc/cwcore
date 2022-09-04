package space.cubicworld.core.database;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
abstract class CoreCache<K, S, M> {

    protected abstract S getSecondary(M model);

    protected abstract K getKey(M model);

    protected abstract M fetch(ResultSet resultSet) throws SQLException;

    protected abstract Connection getConnection();

    protected abstract String getKeyStatement();

    protected abstract String getSecondaryStatement();

    protected abstract void updateDatabase(M model) throws SQLException;

    protected abstract void removeDatabase(M model) throws SQLException;

    private static final Duration DEFAULT_DURATION = Duration.ofHours(1);

    private final Cache<K, Optional<M>> modelCache = CacheBuilder
            .newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(DEFAULT_DURATION)
            .removalListener(this::removalListener)
            .build();
    private final Map<S, Optional<K>> secondaryCache = new ConcurrentHashMap<>();

    private void removalListener(RemovalNotification<K, Optional<M>> notification) {
        notification.getValue().ifPresent(model ->
                secondaryCache.remove(getSecondary(model))
        );
    }

    public Optional<M> fetchByKey(K key) {
        return modelCache.asMap()
                .computeIfAbsent(key, this::fetchNewByKey);
    }

    @SneakyThrows
    private Optional<M> fetchNewByKey(K key) {
        Optional<M> result;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(getKeyStatement())) {
            statement.setObject(1, key instanceof UUID ? key.toString() : key);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) result = Optional.ofNullable(fetch(resultSet));
            else result = Optional.empty();
            resultSet.close();
        }
        result.ifPresent(model -> secondaryCache.put(
                getSecondary(model),
                Optional.of(key)
        ));
        return result;
    }

    public Optional<M> fetchBySecondary(S secondary) {
        return secondaryCache
                .computeIfAbsent(secondary, this::fetchKeyBySecondary)
                .flatMap(this::fetchByKey);
    }

    @SneakyThrows
    private Optional<K> fetchKeyBySecondary(S secondary) {
        Optional<K> key;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(getSecondaryStatement())) {
            statement.setObject(1, secondary instanceof UUID ? secondary.toString() : secondary);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                M model = fetch(resultSet);
                K modelKey = getKey(model);
                modelCache.asMap().putIfAbsent(modelKey, Optional.ofNullable(model));
                key = Optional.of(modelKey);
            } else key = Optional.empty();
        }
        return key;
    }

    public List<M> justFetch(String sql, Object... objects) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int counter = 0;
            for (Object object : objects) {
                statement.setObject(++counter, object instanceof UUID ? object.toString() : object);
            }
            ResultSet resultSet = statement.executeQuery();
            List<M> result = new ArrayList<>();
            while (resultSet.next()) {
                M model = fetch(resultSet);
                result.add(model);
                cacheIfNeed(model);
            }
            resultSet.close();
            return Collections.unmodifiableList(result);
        }
    }

    public void remove(M model) throws SQLException {
        removeDatabase(model);
        removeCache(model);
    }

    private void removeCache(M model) {
        modelCache.put(getKey(model), Optional.empty());
        secondaryCache.put(getSecondary(model), Optional.empty());
    }

    public void update(M model) throws SQLException {
        updateDatabase(model);
    }

    public void cacheIfNeed(M model) {
        K key = getKey(model);
        S secondary = getSecondary(model);
        if (modelCache.getIfPresent(key) == null) modelCache.put(key, Optional.ofNullable(model));
        secondaryCache.putIfAbsent(secondary, Optional.of(key));
    }

    public void cache(M model) {
        modelCache.put(getKey(model), Optional.of(model));
        secondaryCache.put(getSecondary(model), Optional.of(getKey(model)));
    }

    public void changeSecondaryKey(S previousKey, S newKey) {
        secondaryCache.put(newKey, secondaryCache.remove(previousKey));
    }

}
