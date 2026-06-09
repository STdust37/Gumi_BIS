package storage;

import java.time.LocalDateTime;

/**
 * 캐시 생성 시각과 개수 정보 모델
 */
public class CacheMetadata {
    private final String version;
    private final LocalDateTime cachedAt;

    public CacheMetadata(String version, LocalDateTime cachedAt) {
        // 캐시 파일 형식 버전 보관
        this.version = version;

        // 캐시 생성 시각 보관
        this.cachedAt = cachedAt;
    }

    public String getVersion() {
        // 캐시 버전 반환
        return version;
    }

    public LocalDateTime getCachedAt() {
        // 캐시 생성 시각 반환
        return cachedAt;
    }
}
