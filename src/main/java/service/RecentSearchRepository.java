package service;

import java.util.ArrayList;

/**
 * 최근 검색 기록 저장소 계약 역할
 */
public interface RecentSearchRepository {
    void add(String query);

    void remove(String entry);

    ArrayList<String> snapshot();
}
