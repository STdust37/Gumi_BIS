package model;

import java.util.HashMap;

public class HotPlace {
    private final String id;
    private final String name;
    private final String category;
    private final String address;
    private final String longitude;
    private final String latitude;
    private final int mentionCount;
    private final int reviewCount;
    private final double rating;
    private final double score;

    public HotPlace(String id, String name, String category, String address, String longitude, String latitude,
                    int reviewCount, double rating, double score) {
        this(id, name, category, address, longitude, latitude, 0, reviewCount, rating, score);
    }

    public HotPlace(String id, String name, String category, String address, String longitude, String latitude,
                    int mentionCount, int reviewCount, double rating, double score) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.address = address;
        this.longitude = longitude;
        this.latitude = latitude;
        this.mentionCount = mentionCount;
        this.reviewCount = reviewCount;
        this.rating = rating;
        this.score = score;
    }

    public static HotPlace fromMap(HashMap<String, String> map, int rank, int maxRank) {
        String name = firstValue(map, "name", "title", "displayName");
        String category = firstValue(map, "category", "categoryName", "categoryPath", "categoryText");
        String address = firstValue(map, "roadAddress", "address", "fullAddress", "jibunAddress");
        String longitude = firstValue(map, "x", "longitude", "lng", "mapx");
        String latitude = firstValue(map, "y", "latitude", "lat", "mapy");
        int visitorReviews = parseCount(firstValue(map, "visitorReviewCount", "visitorReviewsTotal", "reviewCount"));
        int blogReviews = parseCount(firstValue(map, "blogCafeReviewCount", "blogReviewCount"));
        int reviewCount = visitorReviews + blogReviews;
        double rating = parseDouble(firstValue(map, "rating", "score", "reviewScore"));

        double rankScore = maxRank <= 1 ? 100.0 : Math.max(0.0, 100.0 - ((rank - 1) * (100.0 / maxRank)));
        double reviewScore = reviewCount <= 0 ? 0.0 : Math.min(100.0, Math.log(reviewCount + 1) / Math.log(10001) * 100.0);
        double ratingScore = rating <= 0.0 ? 0.0 : Math.min(100.0, rating / 5.0 * 100.0);
        double score;
        if (reviewCount <= 0 && rating <= 0.0) {
            score = rankScore;
        } else {
            score = (rankScore * 0.5) + (reviewScore * 0.3) + (ratingScore * 0.2);
        }

        return new HotPlace(firstValue(map, "id", "placeId"), stripTags(name), stripTags(category), stripTags(address),
                longitude, latitude, reviewCount, rating, score);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getAddress() {
        return address;
    }

    public String getLongitude() {
        return longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public int getMentionCount() {
        return mentionCount;
    }

    public double getRating() {
        return rating;
    }

    public double getScore() {
        return score;
    }

    public HotPlace withMetrics(int mentionCount, int reviewCount, double rating) {
        return new HotPlace(id, name, category, address, longitude, latitude, mentionCount, reviewCount, rating, score);
    }

    public HotPlace withScore(double score) {
        return new HotPlace(id, name, category, address, longitude, latitude, mentionCount, reviewCount, rating, score);
    }

    private static String firstValue(HashMap<String, String> map, String... keys) {
        for (String key : keys) {
            String value = map.getOrDefault(key, "");
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static int parseCount(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static String stripTags(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("<[^>]*>", "").trim();
    }
}
