package cabe7;

import org.jspecify.annotations.Nullable;

import java.lang.ref.Cleaner;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class Cache<K, V> {

    private static final Cleaner CLEANER = Cleaner.create();

    private final Object lock = new Object();
    private final Function<V, Reference<V>> newReference;
    private final Function<? super K, ? extends V> compute;
    private final Map<K, Reference<V>> items = new ConcurrentHashMap<>();

    public Cache(ReferenceType type, Function<? super K, ? extends V> compute) {
        this.compute = compute;
        this.newReference = switch (type) {
            case SOFT_REFERENCES -> SoftReference::new;
            case WEAK_REFERENCES -> WeakReference::new;
        };
    }

    public V get(@Nullable K key) {
        if (key == null) {
            return null;
        }

        Reference<V> weak = items.get(key);
        V item = weak == null ? null : weak.get();

        if (item == null) {
            synchronized (lock) {
                // Check again within synchronized block
                weak = items.get(key);
                item = weak == null ? null : weak.get();
                if (item == null) {
                    item = compute.apply(key);
                    Reference<V> ref = newReference.apply(item);
                    CLEANER.register(item, () -> items.remove(key));
                    items.put(key, ref);
                }
            }
        }

        return item;
    }

    @Override
    public String toString() {
        return String.format("Cache backed by %s [%d entries]", items.getClass().getSimpleName(), items.size());
    }

    public enum ReferenceType {
        SOFT_REFERENCES,
        WEAK_REFERENCES
    }
}
