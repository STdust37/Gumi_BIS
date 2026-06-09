package storage;

import java.nio.file.Path;

/**
 * 저장 파일 경로 결정 보조 역할
 */
public class StoragePathResolver {
    private final Path root;

    public StoragePathResolver(Path root) {
        // 캐시와 저장 파일의 기준 폴더 보관
        this.root = root;
    }

    public Path resolve(String fileName) {
        // 기준 폴더와 파일명을 합친 실제 경로 생성
        return root.resolve(fileName);
    }

    public Path getRoot() {
        // 기준 폴더 경로 반환
        return root;
    }
}
