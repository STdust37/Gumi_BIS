package service;

import model.HotPlace;

/**
 * 핫플레이스 점수 계산 기본 구현
 */
public class DefaultHotPlaceScorer implements HotPlaceScorer {
    private final HotPlaceClassifier classifier;

    public DefaultHotPlaceScorer(HotPlaceClassifier classifier) {
        this.classifier = classifier;
    }

    @Override
    public HotPlace score(HotPlace hotPlace, HotPlaceScoreContext context) {
        double mentionScore = logNormalize(hotPlace.getMentionCount(), context.getMaxMentionCount());
        double reviewScore = logNormalize(hotPlace.getReviewCount(), context.getMaxReviewCount());
        double ratingScore = hotPlace.getRating() <= 0.0 ? 0.0 : hotPlace.getRating() / 5.0 * 100.0;
        double categoryScore = classifier.isAttraction(hotPlace) ? 100.0 : classifier.isFood(hotPlace) ? 55.0 : 70.0;
        double score = context.isGoogleAvailable()
                ? mentionScore * 0.4 + reviewScore * 0.25 + ratingScore * 0.2 + categoryScore * 0.15
                : mentionScore * 0.65 + categoryScore * 0.35;
        return hotPlace.withScore(score);
    }

    private double logNormalize(int value, int maxValue) {
        if (value <= 0 || maxValue <= 0) {
            return 0.0;
        }
        return Math.log(value + 1.0) / Math.log(maxValue + 1.0) * 100.0;
    }
}
