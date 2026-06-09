package model;

public class HotPlaceRanking {
    private final int rank;
    private final HotPlace hotPlace;

    public HotPlaceRanking(int rank, HotPlace hotPlace) {
        this.rank = rank;
        this.hotPlace = hotPlace;
    }

    public int getRank() {
        return rank;
    }

    public HotPlace getHotPlace() {
        return hotPlace;
    }
}
