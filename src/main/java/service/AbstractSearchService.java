package service;

import java.util.ArrayList;

/**
 * 검색어 정규화와 빈 검색 처리 공통 구현
 */
public abstract class AbstractSearchService<T> implements SearchService<T> {
    @Override
    public final ArrayList<T> search(String keyword) {
        // 공통 검색어 정리
        String normalizedKeyword = normalize(keyword);

        // 빈 검색어는 빈 결과로 처리
        if (normalizedKeyword.isBlank()) {
            return new ArrayList<>();
        }

        // 실제 검색 기준은 자식 클래스에서 구현
        return searchNormalized(normalizedKeyword);
    }

    protected abstract ArrayList<T> searchNormalized(String normalizedKeyword);

    protected String normalize(String value) {
        // null 입력과 앞뒤 공백을 안전하게 처리
        return value == null ? "" : value.trim().toLowerCase();
    }
}
