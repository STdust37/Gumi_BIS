package model;

public class HotPlaceScore {
    private final double mentionScore;
    private final double reviewScore;
    private final double ratingScore;
    private final double categoryScore;

    public HotPlaceScore(double mentionScore, double reviewScore, double ratingScore, double categoryScore) {
        this.mentionScore = mentionScore;
        this.reviewScore = reviewScore;
        this.ratingScore = ratingScore;
        this.categoryScore = categoryScore;
    }

    public double total() {
        return mentionScore + reviewScore + ratingScore + categoryScore;
    }
}
