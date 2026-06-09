package storage;

import java.util.ArrayList;

/**
 * 문자열 JSON 파일 저장 구현
 */
public class JsonFileStore extends AbstractFileStorage<String> {
    private final ArrayList<String> values = new ArrayList<>();

    public JsonFileStore(java.nio.file.Path path) {
        super(path);
    }

    @Override
    public void save(String value) {
        // null 값은 빈 문자열로 바꾸어 저장
        values.add(value == null ? "" : value);
    }

    @Override
    public ArrayList<String> loadAll() {
        // 내부 JSON 문자열 목록 보호를 위한 복사본 반환
        return new ArrayList<>(values);
    }
}
