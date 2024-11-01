package cabe7;

import cabe7.Cache.ReferenceType;
import java.util.function.Function;

public class ObjectCache {

    private final Cache<Object, Object> cache;

    public ObjectCache() {
        cache = new Cache<>(ReferenceType.WEAK_REFERENCES, Function.identity());
    }

    @SuppressWarnings("unchecked")
    public <T> T get(T item) {
        return (T) cache.get(item);
    }
}
