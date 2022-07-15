package space.cubicworld.core.model;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CoreModelCaching<K, V> {

    private static class LoadingTask {

        @Getter
        private final Object lock = new Object();
        private boolean cancelled = false;

        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }

    private final Map<K, V> permanentModels = new ConcurrentHashMap<>();
    private final Map<K, LoadingTask> loadingTasks = new ConcurrentHashMap<>();
    private final Cache<K, V> cachedModels;
    private final BiConsumer<K, Consumer<V>> loadingFunction;
    public CoreModelCaching(BiConsumer<K, Consumer<V>> loadingFunction) {
        cachedModels = CacheBuilder.newBuilder().expireAfterAccess(Duration.ofMinutes(30)).maximumSize(100).build();
        this.loadingFunction = loadingFunction;
    }

    public void loadPermanent(K key, Consumer<V> valueConsumer) {
        load(key, value -> {
            cachedModels.invalidate(key);
            permanentModels.put(key, value);
        }, valueConsumer);
    }

    public void loadCache(K key, Consumer<V> valueConsumer) {
        load(key, value -> cachedModels.put(key, value), valueConsumer);
    }

    public void loadPermanent(K key) {
        loadPermanent(key, value -> {
        });
    }

    public void loadCache(K key) {
        loadPermanent(key, value -> {
        });
    }

    private void load(K key, Consumer<V> putValueConsumer, Consumer<V> valueConsumer) {
        get(key).ifPresentOrElse(valueConsumer, () -> {
            LoadingTask task = new LoadingTask();
            loadingTasks.put(key, task);
            loadingFunction.accept(key, value -> {
                synchronized (task.getLock()) {
                    if (task.isCancelled()) return;
                    if (value == null) {
                        loadingTasks.remove(key);
                        return;
                    }
                    putValueConsumer.accept(value);
                    loadingTasks.remove(key);
                    valueConsumer.accept(value);
                }
            });
        });
    }

    public void unload(K key) {
        LoadingTask task = loadingTasks.get(key);
        if (task != null) {
            synchronized (task.getLock()) {
                task.setCancelled(true);
            }
        }
        permanentModels.remove(key);
        cachedModels.invalidate(key);
    }

    public Optional<V> get(K key) {
        return Optional.ofNullable(permanentModels.get(key))
                .or(() -> Optional.ofNullable(cachedModels.getIfPresent(key)));
    }

}
