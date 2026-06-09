package model;

public class HotPlaceCategoryProfile {
    private final String categoryName;
    private final boolean attraction;
    private final boolean food;

    public HotPlaceCategoryProfile(String categoryName, boolean attraction, boolean food) {
        this.categoryName = categoryName;
        this.attraction = attraction;
        this.food = food;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public boolean isAttraction() {
        return attraction;
    }

    public boolean isFood() {
        return food;
    }
}
