package service;

/**
 * 핫플레이스 점수 계산 입력값 모델
 */
public class HotPlaceScoreContext {
    private final int maxMentionCount;
    private final int maxReviewCount;
    private final boolean googleAvailable;

    public HotPlaceScoreContext(int maxMentionCount, int maxReviewCount, boolean googleAvailable) {
        this.maxMentionCount = maxMentionCount;
        this.maxReviewCount = maxReviewCount;
        this.googleAvailable = googleAvailable;
    }

    public int getMaxMentionCount() {
        return maxMentionCount;
    }

    public int getMaxReviewCount() {
        return maxReviewCount;
    }

    public boolean isGoogleAvailable() {
        return googleAvailable;
    }
}
