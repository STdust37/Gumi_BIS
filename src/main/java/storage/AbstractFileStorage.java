package storage;

import java.nio.file.Path;

/**
 * 파일 기반 저장소 공통 읽기 쓰기 흐름 구현
 */
public abstract class AbstractFileStorage<T> implements Repository<T> {
    private final Path path;

    protected AbstractFileStorage(Path path) {
        // 저장소가 사용할 파일 경로 보관
        this.path = path;
    }

    public Path getPath() {
        // 실제 저장 경로 조회
        return path;
    }

    protected boolean hasPath() {
        // 파일 경로가 설정되어 있는지 확인
        return path != null;
    }
}
