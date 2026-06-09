package storage;

import java.nio.file.Path;
import java.util.ArrayList;

/**
 * 즐겨찾기 파일 저장 구현
 */
public class FavoriteFileStorage extends AbstractFileStorage<String> {
    private final ArrayList<String> favorites = new ArrayList<>();

    public FavoriteFileStorage(Path path) {
        super(path);
    }

    @Override
    public void save(String value) {
        // 빈 즐겨찾기 값은 저장 대상에서 제외
        if (value != null && !value.isBlank()) {
            favorites.add(value);
        }
    }

    @Override
    public ArrayList<String> loadAll() {
        // 내부 즐겨찾기 목록 보호를 위한 복사본 반환
        return new ArrayList<>(favorites);
    }
}
