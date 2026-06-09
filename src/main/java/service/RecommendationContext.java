package service;

/**
 * 추천 계산에 필요한 기준 정보 모델
 */
public class RecommendationContext {
    private final int destinationCandidateCount;
    private final int maxResultCount;

    public RecommendationContext(int destinationCandidateCount, int maxResultCount) {
        this.destinationCandidateCount = destinationCandidateCount;
        this.maxResultCount = maxResultCount;
    }

    public int getDestinationCandidateCount() {
        return destinationCandidateCount;
    }

    public int getMaxResultCount() {
        return maxResultCount;
    }
}
