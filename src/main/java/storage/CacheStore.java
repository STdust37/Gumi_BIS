package storage;

/**
 * 캐시 저장소 공통 계약 역할
 */
public interface CacheStore<K, V> {
    boolean contains(K key);

    V get(K key);

    void put(K key, V value);

    void clear();
}
