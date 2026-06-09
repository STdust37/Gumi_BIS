package service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/**
 * 메모리 기반 최근 검색 기록 관리 구현
 */
public class RecentSearchStore implements RecentSearchRepository {
    private static final int MAX_HISTORY = 20;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ArrayList<String> history = new ArrayList<>();

    public void add(String query) {
        // 빈 검색어는 최근 검색에 저장하지 않는 처리
        if (query == null || query.isBlank()) {
            return;
        }

        // 같은 검색어가 이미 있으면 기존 기록 제거
        String duplicatedSuffix = " | " + query;
        for (int i = history.size() - 1; i >= 0; i--) {
            String entry = history.get(i);
            if (entry.endsWith(duplicatedSuffix)) {
                history.remove(i);
            }
        }

        // 현재 시각과 검색어를 하나의 문자열로 저장
        String entry = FORMATTER.format(LocalDateTime.now()) + " | " + query;
        history.add(0, entry);

        // 최대 개수를 넘으면 가장 오래된 기록부터 삭제
        while (history.size() > MAX_HISTORY) {
            history.remove(history.size() - 1);
        }
    }

    public void remove(String entry) {
        // 사용자가 x 버튼을 누른 최근 검색 항목 삭제
        history.remove(entry);
    }

    public ArrayList<String> snapshot() {
        // 화면 표시용 복사본 반환
        return new ArrayList<>(history);
    }
}
