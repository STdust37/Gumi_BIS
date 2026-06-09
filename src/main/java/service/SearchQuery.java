package service;

/**
 * 검색어 입력값 표현 모델
 */
public class SearchQuery {
    private final String keyword;

    public SearchQuery(String keyword) {
        this.keyword = keyword == null ? "" : keyword.trim();
    }

    public String getKeyword() {
        return keyword;
    }

    public boolean isBlank() {
        return keyword.isBlank();
    }
}
