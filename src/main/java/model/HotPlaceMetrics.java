package model;

public class HotPlaceMetrics {
    private final int mentionCount;
    private final int reviewCount;
    private final double rating;

    public HotPlaceMetrics(int mentionCount, int reviewCount, double rating) {
        this.mentionCount = mentionCount;
        this.reviewCount = reviewCount;
        this.rating = rating;
    }

    public int getMentionCount() {
        return mentionCount;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public double getRating() {
        return rating;
    }
}
