package model;

public class HotPlaceTransitOption {
    private final HotPlace hotPlace;
    private final BusStop departureStop;
    private final NearbyStopCandidate destinationStop;
    private final ArrivalInfo arrivalInfo;
    private final int stopsBetween;
    private final double score;

    public HotPlaceTransitOption(HotPlace hotPlace, BusStop departureStop, NearbyStopCandidate destinationStop,
                                 ArrivalInfo arrivalInfo, int stopsBetween, double score) {
        this.hotPlace = hotPlace;
        this.departureStop = departureStop;
        this.destinationStop = destinationStop;
        this.arrivalInfo = arrivalInfo;
        this.stopsBetween = stopsBetween;
        this.score = score;
    }

    public HotPlace getHotPlace() {
        return hotPlace;
    }

    public BusStop getDepartureStop() {
        return departureStop;
    }

    public NearbyStopCandidate getDestinationStop() {
        return destinationStop;
    }

    public ArrivalInfo getArrivalInfo() {
        return arrivalInfo;
    }

    public String getRouteId() {
        return arrivalInfo.getRouteId();
    }

    public String getBrtId() {
        return arrivalInfo.getBrtId();
    }

    public int getStopsBetween() {
        return stopsBetween;
    }

    public double getScore() {
        return score;
    }
}
