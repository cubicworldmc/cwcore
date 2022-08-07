package space.cubicworld.core.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import space.cubicworld.core.CorePlugin;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class CoreSecondaryCache<S, K, M> {

    private final LoadingCache<S, K> secondaryToPrimary;
    private final CorePlugin plugin;
    private final Class<M> modelClass;

    public CoreSecondaryCache(CorePlugin plugin, Class<M> modelClass, String selectStatement, Function<M, K> keyGetter) {
        this.plugin = plugin;
        this.modelClass = modelClass;
        secondaryToPrimary = CacheBuilder
                .newBuilder()
                .expireAfterAccess(Duration.ofMinutes(30))
                .maximumSize(1000)
                .build(CacheLoader.from(key -> {
                    plugin.beginTransaction();
                    M model = plugin.getHibernateSessionFactory()
                            .getCurrentSession()
                            .createQuery(selectStatement, modelClass)
                            .setParameter("key", key)
                            .getSingleResult();
                    return keyGetter.apply(model);
                }));
    }

    public K getPrimary(S secondary) throws ExecutionException {
        return secondaryToPrimary.get(secondary);
    }

    public Optional<K> getOptionalPrimary(S secondary) {
        try {
            return Optional.of(getPrimary(secondary));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public M getModel(S secondary) throws ExecutionException {
        plugin.beginTransaction();
        return plugin
                .getHibernateSessionFactory()
                .getCurrentSession()
                .get(modelClass, getPrimary(secondary));
    }

    public Optional<M> getOptionalModel(S secondary) {
        try {
            return Optional.of(getModel(secondary));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void cache(K key, S secondary) {
        secondaryToPrimary.put(secondary, key);
    }

    public void unCache(S secondary) {
        secondaryToPrimary.invalidate(secondary);
    }

}
