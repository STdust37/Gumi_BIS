package storage;

import java.nio.file.Path;
import java.util.ArrayList;

/**
 * 최근 검색 기록 파일 저장 구현
 */
public class RecentSearchFileStorage extends AbstractFileStorage<String> {
    private final ArrayList<String> history = new ArrayList<>();

    public RecentSearchFileStorage(Path path) {
        super(path);
    }

    @Override
    public void save(String value) {
        // 빈 검색어는 저장 대상에서 제외
        if (value != null && !value.isBlank()) {
            history.add(0, value);
        }
    }

    @Override
    public ArrayList<String> loadAll() {
        // 내부 최근 검색 목록 보호를 위한 복사본 반환
        return new ArrayList<>(history);
    }
}
