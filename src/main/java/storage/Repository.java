package storage;

import java.util.ArrayList;

/**
 * 저장소 읽기 쓰기 공통 계약 역할
 */
public interface Repository<T> {
    void save(T value);

    ArrayList<T> loadAll();
}
